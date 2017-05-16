package com.jetbrains.pluginverifier.utils

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.impl.domain.PluginImpl
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.warnings.Warning
import org.jgrapht.DirectedGraph
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class VerificationWorker(val pluginDescriptor: PluginDescriptor,
                         val ide: Ide,
                         val ideResolver: Resolver,
                         val runtimeResolver: Resolver,
                         val params: VerifierParams) : Callable<Result> {

  private val dependencyResolver = params.dependencyResolver ?: DefaultDependencyResolver(ide)

  private val graphBuilder = DepGraphBuilder(dependencyResolver)

  private lateinit var plugin: Plugin

  private lateinit var pluginResolver: Resolver

  companion object {
    private val LOG = LoggerFactory.getLogger(VerificationWorker::class.java)
  }

  override fun call(): Result {
    if (Thread.currentThread().isInterrupted) {
      throw InterruptedException()
    }
    try {
      return createAndVerifyPlugin()
    } catch (e: Throwable) {
      LOG.error("Unable to verify $pluginDescriptor with $ide", e)
      throw e
    }
  }

  private fun createAndVerifyPlugin(): Result {
    val createPluginResult = VerificationUtil.createPluginAndResolver(pluginDescriptor, ide.version)
    createPluginResult.use {
      val (pluginInstance, resolver, badCreation) = createPluginResult
      if (badCreation != null) {
        return badCreation
      }

      plugin = pluginInstance!!
      pluginResolver = resolver!!

      LOG.info("Verifying $plugin with $ide")
      return verifyPlugin()
    }
  }

  fun verifyPlugin(): Result {
    val (graph, start) = graphBuilder.build(plugin, pluginResolver)
    val context: VerificationContext = try {
      runVerifier(graph)
    } finally {
      graph.vertexSet().mapNotNull { it.fileLock }.forEach { it.release() }
    }

    val apiGraph = DepGraph2ApiGraphConverter.convert(graph, start)
    addCycleAndOtherWarnings(apiGraph, context)
    return getAppropriateResult(context, apiGraph)
  }

  private fun addCycleAndOtherWarnings(apiGraph: DependenciesGraph, context: VerificationContext) {
    val cycles = apiGraph.getCycles()
    if (cycles.isNotEmpty()) {
      val nodes = cycles[0]
      val cycle = nodes.joinToString(separator = " -> ") + " -> " + nodes[0]
      context.registerWarning(Warning("The plugin $plugin is on the dependencies cycle: $cycle"))
    }

    //todo: replace with new structure API
    val pluginImpl = plugin as? PluginImpl
    if (pluginImpl != null && pluginImpl.hints.isNotEmpty()) {
      pluginImpl.hints.forEach {
        context.registerWarning(Warning(it))
      }
    }
  }

  private fun runVerifier(graph: DirectedGraph<DepVertex, DepEdge>): VerificationContext {
    val resolver = getDependenciesClassesResolver(graph)
    val checkClasses = getClassesForCheck()
    val classLoader = createClassLoader(resolver, ideResolver, pluginResolver, runtimeResolver, params.externalClassPath, ide, plugin)
    classLoader.use {
      return runVerifier(classLoader, ide, params, plugin, checkClasses)
    }
  }

  private fun createClassLoader(dependenciesResolver: Resolver,
                                ideResolver: Resolver,
                                pluginResolver: Resolver,
                                runtimeResolver: Resolver,
                                externalClassPath: Resolver,
                                ide: Ide, plugin: Plugin): Resolver =
      Resolver.createCacheResolver(
          Resolver.createUnionResolver(
              "Common resolver for plugin " + plugin.pluginId + " with its transitive dependencies; ide " + ide.version + "; jdk " + runtimeResolver,
              listOf(pluginResolver, runtimeResolver, ideResolver, dependenciesResolver, externalClassPath)
          )
      )

  private fun getClassesForCheck(): Set<String> {
    val resolver = Resolver.createUnionResolver("Plugin classes for check",
        (plugin.allClassesReferencedFromXml + plugin.optionalDescriptors.flatMap { it.value.allClassesReferencedFromXml })
            .map { pluginResolver.getClassLocation(it) }
            .filterNotNull()
            .distinct())
    return if (resolver.isEmpty) pluginResolver.allClasses else resolver.allClasses
  }

  private fun getDependenciesClassesResolver(graph: DirectedGraph<DepVertex, DepEdge>): Resolver =
      Resolver.createUnionResolver("Plugin $plugin dependencies resolvers", graph.vertexSet().map { it.resolver }.filterNotNull())

  private fun getPluginInfoByDescriptor(pluginDescriptor: PluginDescriptor): PluginInfo = when (pluginDescriptor) {
    is PluginDescriptor.ByUpdateInfo -> PluginInfo(pluginDescriptor.pluginId, pluginDescriptor.version, pluginDescriptor.updateInfo)
    else -> PluginInfo(pluginDescriptor.pluginId, pluginDescriptor.version, null)
  }

  private fun getAppropriateResult(ctx: VerificationContext, dependenciesGraph: DependenciesGraph): Result {
    val ideVersion = ide.version
    val pluginInfo = getPluginInfoByDescriptor(pluginDescriptor)
    return Result(pluginInfo, ideVersion, getAppropriateVerdict(ctx, dependenciesGraph))
  }

  private fun getAppropriateVerdict(context: VerificationContext, dependenciesGraph: DependenciesGraph): Verdict {
    val missingDependencies = dependenciesGraph.start.missingDependencies
    if (missingDependencies.isNotEmpty()) {
      return Verdict.MissingDependencies(missingDependencies, dependenciesGraph, context.problems, context.warnings)
    }

    if (context.problems.isNotEmpty()) {
      return Verdict.Problems(context.problems, dependenciesGraph, context.warnings)
    }

    if (context.warnings.isNotEmpty()) {
      return Verdict.Warnings(context.warnings, dependenciesGraph)
    }

    return Verdict.OK(dependenciesGraph)
  }

  private fun runVerifier(classLoader: Resolver,
                          ide: Ide,
                          params: VerifierParams,
                          plugin: Plugin,
                          checkClasses: Set<String>): VerificationContext {
    val context = VerificationContext(plugin, ide, params, classLoader)
    BytecodeVerifier(context).verify(checkClasses, classLoader)
    return context
  }

}