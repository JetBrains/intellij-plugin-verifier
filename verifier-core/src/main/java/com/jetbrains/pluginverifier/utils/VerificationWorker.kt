package com.jetbrains.pluginverifier.utils

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.warnings.Warning
import org.jgrapht.DirectedGraph
import java.util.concurrent.Callable

class VerificationWorker(val pluginDescriptor: PluginDescriptor,
                         val ideDescriptor: IdeDescriptor,
                         val ide: Ide,
                         val ideResolver: Resolver,
                         val runtimeResolver: Resolver,
                         val params: VerifierParams) : Callable<VerificationResult> {

  private val dependencyResolver = params.dependencyResolver ?: DefaultDependencyResolver(ide)

  private val graphBuilder = DepGraphBuilder(dependencyResolver)

  private lateinit var plugin: Plugin
  private lateinit var pluginResolver: Resolver
  private lateinit var warnings: List<PluginProblem>

  override fun call(): VerificationResult {
    val createPluginResult = PluginCreator.createPlugin(pluginDescriptor)
    createPluginResult.use {
      return when (createPluginResult) {
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
    }
  }

  private fun getPluginInfo(createPluginResult: CreatePluginResult.OK, pluginDescriptor: PluginDescriptor): PluginInfo =
      PluginInfo(createPluginResult.success.plugin.pluginId, createPluginResult.success.plugin.pluginVersion, (pluginDescriptor as? PluginDescriptor.ByUpdateInfo)?.updateInfo)

  fun getVerificationVerdict(creationOk: CreatePluginResult.OK): Verdict {
    plugin = creationOk.success.plugin
    warnings = creationOk.success.warnings
    pluginResolver = creationOk.resolver

    val (graph, start) = graphBuilder.build(creationOk)
    val context: VerificationContext = try {
      runVerifier(graph)
    } finally {
      graph.vertexSet().forEach { it.closeLogged() }
    }

    val apiGraph = DepGraph2ApiGraphConverter.convert(graph, start)
    addCycleAndOtherWarnings(apiGraph, context)
    return getAppropriateVerdict(context, apiGraph)
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
    val classLoader = createClassLoader(resolver, ideResolver, runtimeResolver, params.externalClassPath, ide)
    classLoader.use {
      return runVerifier(classLoader, ide, params, plugin, checkClasses)
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

  private fun getClassesForCheck(): Set<String> {
    val resolver = Resolver.createUnionResolver("Plugin classes for check",
        (plugin.allClassesReferencedFromXml + plugin.optionalDescriptors.flatMap { it.value.allClassesReferencedFromXml })
            .map { pluginResolver.getClassLocation(it) }
            .filterNotNull()
            .distinct())
    return if (resolver.isEmpty) pluginResolver.allClasses else resolver.allClasses
  }

  private fun getDependenciesClassesResolver(graph: DirectedGraph<DepVertex, DepEdge>): Resolver =
      Resolver.createUnionResolver("Plugin $plugin dependencies resolvers", graph.vertexSet().map { it.creationOk.resolver }.filterNotNull())

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