package com.jetbrains.pluginverifier.core

import com.intellij.structure.plugin.Plugin
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.misc.withDebug
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.utils.CloseIgnoringResolver
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.warnings.Warning
import org.jgrapht.DirectedGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Callable

class Verifier(val pluginDescriptor: PluginDescriptor,
               val ideDescriptor: IdeDescriptor,
               val runtimeResolver: Resolver,
               val params: VerifierParams) : Callable<Result> {

  private lateinit var plugin: Plugin
  private lateinit var pluginResolver: Resolver
  private lateinit var warnings: List<PluginProblem>

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(Verifier::class.java)
  }

  override fun call(): Result {
    withDebug(LOG, "Verify $pluginDescriptor with $ideDescriptor") {
      return createPluginAndDoVerification()
    }
  }

  private fun createPluginAndDoVerification(): Result = PluginCreator.createPlugin(pluginDescriptor).use { createPluginResult ->
    when (createPluginResult) {
      is CreatePluginResult.BadPlugin -> {
        val pluginInfo = getPluginInfoByDescriptor(pluginDescriptor)
        Result(pluginInfo, ideDescriptor.ideVersion, Verdict.Bad(createPluginResult.pluginErrorsAndWarnings))
      }
      is CreatePluginResult.NotFound -> {
        val pluginInfo = getPluginInfoByDescriptor(pluginDescriptor)
        Result(pluginInfo, ideDescriptor.ideVersion, Verdict.NotFound(createPluginResult.reason))
      }
      is CreatePluginResult.OK -> {
        val verdict = getVerificationVerdict(createPluginResult)
        val pluginInfo = getPluginInfoByPluginInstance(createPluginResult, pluginDescriptor)
        Result(pluginInfo, ideDescriptor.ideVersion, verdict)
      }
    }
  }

  private fun getPluginInfoByDescriptor(pluginDescriptor: PluginDescriptor): PluginInfo = when (pluginDescriptor) {
    is PluginDescriptor.ByUpdateInfo -> PluginInfo(pluginDescriptor.updateInfo.pluginId, pluginDescriptor.updateInfo.version, pluginDescriptor.updateInfo)
    is PluginDescriptor.ByFile -> {
      val (pluginId, version) = guessPluginIdAndVersion(pluginDescriptor.pluginFile)
      PluginInfo(pluginId, version, null)
    }
    is PluginDescriptor.ByInstance -> {
      PluginInfo(pluginDescriptor.createOk.plugin.pluginId, pluginDescriptor.createOk.plugin.pluginVersion, null)
    }
  }

  private fun guessPluginIdAndVersion(file: File): Pair<String, String> {
    val name = file.nameWithoutExtension
    val version = name.substringAfterLast('-')
    return name.substringBeforeLast('-') to version
  }

  private fun getPluginInfoByPluginInstance(createPluginResult: CreatePluginResult.OK, pluginDescriptor: PluginDescriptor): PluginInfo {
    val plugin = createPluginResult.plugin
    return PluginInfo(plugin.pluginId, plugin.pluginVersion, (pluginDescriptor as? PluginDescriptor.ByUpdateInfo)?.updateInfo)
  }

  private fun getVerificationVerdict(creationOk: CreatePluginResult.OK): Verdict {
    plugin = creationOk.plugin
    warnings = creationOk.warnings
    pluginResolver = creationOk.resolver

    val dependencyResolver = params.dependencyResolver ?: DefaultDependencyResolver(ideDescriptor.ide)
    DepGraphBuilder(dependencyResolver).use { graphBuilder ->
      val (graph, start) = graphBuilder.build(creationOk)
      val apiGraph = DepGraph2ApiGraphConverter.convert(graph, start)
      LOG.debug("Dependencies graph for $plugin: $apiGraph")
      val context = runVerifier(graph)
      addCycleAndOtherWarnings(apiGraph, context)
      return getAppropriateVerdict(context, apiGraph)
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
    val dependenciesResolver = getDependenciesClassesResolver(graph)
    val checkClasses = getClassesOfPluginToCheck()
    val classLoader = getVerificationClassLoader(dependenciesResolver)
    return classLoader.use {
      BytecodeVerifier(params, plugin, classLoader, ideDescriptor.ideVersion).verify(checkClasses)
    }
  }

  private fun getVerificationClassLoader(dependenciesResolver: Resolver): Resolver =
      CloseIgnoringResolver(Resolver.createCacheResolver(
          Resolver.createUnionResolver(
              "Common resolver for plugin $plugin; IDE #${ideDescriptor.ideVersion}; JDK $runtimeResolver",
              listOf(pluginResolver, runtimeResolver, ideDescriptor.ideResolver, dependenciesResolver, params.externalClassPath)
          )
      ))

  private fun getClassesOfPluginToCheck(): Iterator<String> {
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

}