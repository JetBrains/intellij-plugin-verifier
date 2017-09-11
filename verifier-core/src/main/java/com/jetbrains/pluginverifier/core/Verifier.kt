package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.createUnionResolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.utils.PluginXmlUtil.getAllClassesReferencedFromXml
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.misc.withDebug
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.warnings.Warning
import org.jgrapht.DirectedGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Callable

class Verifier(val pluginCoordinate: PluginCoordinate,
               val ideDescriptor: IdeDescriptor,
               val runtimeResolver: Resolver,
               val params: VerifierParams) : Callable<Result> {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(Verifier::class.java)
  }

  override fun call(): Result {
    withDebug(LOG, "Verify $pluginCoordinate with $ideDescriptor") {
      return createPluginAndDoVerification()
    }
  }

  private fun createPluginAndDoVerification(): Result = PluginCreator.createPlugin(pluginCoordinate).use { createPluginResult ->
    val (pluginInfo, verdict) = getPluginInfoByAndVerdict(createPluginResult)
    Result(pluginInfo, ideDescriptor.ideVersion, verdict)
  }

  private fun getPluginInfoByAndVerdict(createPluginResult: CreatePluginResult) = when (createPluginResult) {
    is CreatePluginResult.BadPlugin -> {
      getPluginInfoByCoordinate(pluginCoordinate) to Verdict.Bad(createPluginResult.pluginErrorsAndWarnings)
    }
    is CreatePluginResult.NotFound -> {
      getPluginInfoByCoordinate(pluginCoordinate) to Verdict.NotFound(createPluginResult.reason)
    }
    is CreatePluginResult.OK -> {
      getPluginInfoByPluginInstance(createPluginResult, pluginCoordinate) to getVerificationVerdict(createPluginResult)
    }
  }

  private fun getPluginInfoByCoordinate(pluginCoordinate: PluginCoordinate): PluginInfo = when (pluginCoordinate) {
    is PluginCoordinate.ByUpdateInfo -> PluginInfo(pluginCoordinate.updateInfo.pluginId, pluginCoordinate.updateInfo.version, pluginCoordinate.updateInfo)
    is PluginCoordinate.ByFile -> {
      val (pluginId, version) = guessPluginIdAndVersion(pluginCoordinate.pluginFile)
      PluginInfo(pluginId, version, null)
    }
  }

  private fun guessPluginIdAndVersion(file: File): Pair<String, String> {
    val name = file.nameWithoutExtension
    val version = name.substringAfterLast('-')
    return name.substringBeforeLast('-') to version
  }

  private fun getPluginInfoByPluginInstance(createPluginResult: CreatePluginResult.OK, pluginCoordinate: PluginCoordinate): PluginInfo {
    val plugin = createPluginResult.plugin
    return PluginInfo(plugin.pluginId!!, plugin.pluginVersion!!, (pluginCoordinate as? PluginCoordinate.ByUpdateInfo)?.updateInfo)
  }

  private fun getVerificationVerdict(creationOk: CreatePluginResult.OK): Verdict {
    val plugin = creationOk.plugin
    val warnings = creationOk.warnings
    val pluginResolver = creationOk.resolver

    val dependencyResolver = params.dependencyResolver ?: IdeDependencyResolver(ideDescriptor.ide)
    DepGraphBuilder(dependencyResolver).use { graphBuilder ->
      val (graph, start) = graphBuilder.build(creationOk.plugin, creationOk.resolver)
      val apiGraph = DepGraph2ApiGraphConverter.convert(graph, start)
      LOG.debug("Dependencies graph for $plugin: $apiGraph")
      val context = runVerifier(graph, plugin, pluginResolver)
      addCycleAndOtherWarnings(apiGraph, context, plugin, warnings)
      return getAppropriateVerdict(context, apiGraph)
    }
  }

  private fun addCycleAndOtherWarnings(apiGraph: DependenciesGraph, context: VerificationContext, plugin: IdePlugin, warnings: List<PluginProblem>) {
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

  private fun runVerifier(graph: DirectedGraph<DepVertex, DepEdge>, plugin: IdePlugin, pluginResolver: Resolver): VerificationContext {
    val dependenciesResolver = getDependenciesClassesResolver(graph)
    val checkClasses = getClassesOfPluginToCheck(plugin, pluginResolver)
    val classLoader = getVerificationClassLoader(dependenciesResolver, plugin, pluginResolver)
    //don't close classLoader because it consists of client-resolvers.
    return BytecodeVerifier(params, plugin, classLoader, ideDescriptor.ideVersion).verify(checkClasses)
  }

  private fun getVerificationClassLoader(dependenciesResolver: Resolver, plugin: IdePlugin, pluginResolver: Resolver): Resolver = CacheResolver(
      createUnionResolver(
          "Common resolver for plugin $plugin; IDE #${ideDescriptor.ideVersion}; JDK $runtimeResolver",
          listOf(pluginResolver, runtimeResolver, ideDescriptor.ideResolver, dependenciesResolver, params.externalClassPath)
      )
  )

  private fun getClassesOfPluginToCheck(plugin: IdePlugin, pluginResolver: Resolver): Iterator<String> {
    val resolver = createUnionResolver("Plugin classes for check",
        (getAllClassesReferencedFromXml(plugin) + plugin.optionalDescriptors.flatMap { getAllClassesReferencedFromXml(it.value) })
            .mapNotNull { pluginResolver.getClassLocation(it) }
            .distinct())
    return if (resolver.isEmpty) pluginResolver.allClasses else resolver.allClasses
  }

  private fun getDependenciesClassesResolver(graph: DirectedGraph<DepVertex, DepEdge>): Resolver {
    val resolvers = graph.vertexSet().mapNotNull { getResolverByResult(it.resolveResult) }
    return createUnionResolver("Plugin dependencies resolver", resolvers)
  }

  private fun getResolverByResult(result: DependencyResolver.Result): Resolver? = when (result) {
    is DependencyResolver.Result.FoundReady -> result.resolver
    is DependencyResolver.Result.CreatedResolver -> result.resolver
    is DependencyResolver.Result.Downloaded -> result.resolver
    is DependencyResolver.Result.ProblematicDependency -> null
    is DependencyResolver.Result.NotFound -> null
    DependencyResolver.Result.Skip -> null
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

}