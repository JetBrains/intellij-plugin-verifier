package com.jetbrains.pluginverifier.utils

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.withDebug
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.warnings.Warning
import org.jgrapht.DirectedGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

class VerificationWorker(val pluginDescriptor: PluginDescriptor,
                         val ideDescriptor: IdeDescriptor,
                         val runtimeResolver: Resolver,
                         val params: VerifierParams) : Callable<VerificationResult> {

  private val dependencyResolver = params.dependencyResolver ?: DefaultDependencyResolver(ideDescriptor.createIdeResult.ide)

  private val graphBuilder = DepGraphBuilder(dependencyResolver)

  private lateinit var plugin: Plugin
  private lateinit var pluginResolver: Resolver
  private lateinit var warnings: List<PluginProblem>

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(VerificationWorker::class.java)
  }

  override fun call(): VerificationResult {
    withDebug(LOG, "Verification $pluginDescriptor with $ideDescriptor") {
      return createPluginAndDoVerification()
    }
  }

  private fun createPluginAndDoVerification(): VerificationResult {
    PluginCreator.createPlugin(pluginDescriptor).use { createPluginResult ->
      return doPluginVerification(createPluginResult)
    }
  }

  private fun doPluginVerification(createPluginResult: CreatePluginResult): VerificationResult = when (createPluginResult) {
    is CreatePluginResult.BadPlugin -> {
      VerificationResult.BadPlugin(pluginDescriptor, ideDescriptor, createPluginResult.pluginCreationFail.errorsAndWarnings)
    }
    is CreatePluginResult.OK -> {
      val verdict = getVerificationVerdict(createPluginResult)
      val pluginInfo = getPluginInfo(createPluginResult, pluginDescriptor)
      VerificationResult.Verified(pluginDescriptor, ideDescriptor, verdict, pluginInfo)
    }
    is CreatePluginResult.NotFound -> {
      VerificationResult.NotFound(pluginDescriptor, ideDescriptor, createPluginResult.reason)
    }
  }

  private fun getPluginInfo(createPluginResult: CreatePluginResult.OK, pluginDescriptor: PluginDescriptor): PluginInfo {
    val plugin = createPluginResult.success.plugin
    return PluginInfo(plugin.pluginId, plugin.pluginVersion, (pluginDescriptor as? PluginDescriptor.ByUpdateInfo)?.updateInfo)
  }

  fun getVerificationVerdict(creationOk: CreatePluginResult.OK): Verdict {
    plugin = creationOk.success.plugin
    warnings = creationOk.success.warnings
    pluginResolver = creationOk.resolver

    val (graph, start) = graphBuilder.build(creationOk)
    try {
      val context = runVerifier(graph)
      val apiGraph = DepGraph2ApiGraphConverter.convert(graph, start)
      addCycleAndOtherWarnings(apiGraph, context)
      return getAppropriateVerdict(context, apiGraph)
    } finally {
      graph.vertexSet().forEach { it.closeLogged() }
    }
  }

  private fun addCycleAndOtherWarnings(apiGraph: DependenciesGraph, context: VerificationContext) {
    val cycles = apiGraph.getCycles()
    if (cycles.isNotEmpty()) {
      val nodes = cycles[0]
      val cycle = nodes.joinToString(separator = " -> ") + " -> " + nodes[0]
      context.registerWarning(Warning("The plugin $plugin is on the dependencies cycle: $cycle"))
    }

    warnings.forEach {
      context.registerWarning(Warning(it.message))
    }
  }

  private fun runVerifier(graph: DirectedGraph<DepVertex, DepEdge>): VerificationContext {
    val resolver = getDependenciesClassesResolver(graph)
    val checkClasses = getClassesForCheck()
    val classLoader = createClassLoader(resolver, ideDescriptor.createIdeResult.ideResolver, runtimeResolver, params.externalClassPath, ideDescriptor.createIdeResult.ide)
    classLoader.use {
      return getVerificationContext(classLoader, ideDescriptor.createIdeResult.ide, params, plugin, checkClasses)
    }
  }

  private fun createClassLoader(dependenciesResolver: Resolver,
                                ideResolver: Resolver,
                                runtimeResolver: Resolver,
                                externalClassPath: Resolver,
                                ide: Ide): Resolver =
      Resolver.createCacheResolver(
          Resolver.createUnionResolver(
              "Common resolver for plugin " + plugin.pluginId + " with its transitive dependencies; ide " + ide.version + "; jdk " + runtimeResolver,
              listOf(pluginResolver, runtimeResolver, ideResolver, dependenciesResolver, externalClassPath)
          )
      )

  private fun getClassesForCheck(): Iterator<String> {
    val resolver = Resolver.createUnionResolver("Plugin classes for check",
        (plugin.allClassesReferencedFromXml + plugin.optionalDescriptors.flatMap { it.value.allClassesReferencedFromXml })
            .map { pluginResolver.getClassLocation(it) }
            .filterNotNull()
            .distinct())
    return if (resolver.isEmpty) pluginResolver.allClasses else resolver.allClasses
  }

  private fun getDependenciesClassesResolver(graph: DirectedGraph<DepVertex, DepEdge>): Resolver =
      Resolver.createUnionResolver("Plugin $plugin dependencies resolvers", graph.vertexSet().map { it.creationOk.resolver })

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

  private fun getVerificationContext(classLoader: Resolver,
                                     ide: Ide,
                                     params: VerifierParams,
                                     plugin: Plugin,
                                     checkClasses: Iterator<String>): VerificationContext {
    val context = VerificationContext(plugin, ide, params, classLoader)
    BytecodeVerifier(context).verify(checkClasses)
    return context
  }

}