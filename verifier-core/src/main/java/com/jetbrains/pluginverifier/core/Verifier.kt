package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
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
               val params: VerifierParams,
               val pluginCreator: PluginCreator) : Callable<Result> {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(Verifier::class.java)
  }

  override fun call(): Result {
    withDebug(LOG, "Verify $pluginCoordinate with $ideDescriptor") {
      return createPluginAndDoVerification()
    }
  }

  private fun createPluginAndDoVerification(): Result = pluginCreator.createPlugin(pluginCoordinate).use { createPluginResult ->
    val (pluginInfo, verdict) = getPluginInfoAndVerdict(createPluginResult)
    Result(pluginInfo, ideDescriptor.ideVersion, verdict)
  }

  private fun getPluginInfoAndVerdict(createPluginResult: CreatePluginResult) = when (createPluginResult) {
    is CreatePluginResult.BadPlugin -> {
      getPluginInfoByCoordinate(pluginCoordinate) to Verdict.Bad(createPluginResult.pluginErrorsAndWarnings)
    }
    is CreatePluginResult.NotFound -> {
      getPluginInfoByCoordinate(pluginCoordinate) to Verdict.NotFound(createPluginResult.reason)
    }
    is CreatePluginResult.OK -> {
      getPluginInfoByPluginInstance(createPluginResult, pluginCoordinate) to calculateVerdict(createPluginResult)
    }
    is CreatePluginResult.FailedToDownload -> {
      getPluginInfoByCoordinate(pluginCoordinate) to Verdict.FailedToDownload(createPluginResult.reason)
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

  private fun calculateVerdict(creationOk: CreatePluginResult.OK): Verdict {
    val plugin = creationOk.plugin
    val warnings = creationOk.warnings
    val pluginClassesLocations = creationOk.pluginClassesLocations

    val dependencyResolver = params.dependencyResolver
    DepGraphBuilder(dependencyResolver).use { graphBuilder ->
      val (graph, start) = graphBuilder.build(creationOk.plugin, creationOk.pluginClassesLocations)
      val apiGraph = DepGraph2ApiGraphConverter.convert(graph, start)
      LOG.debug("Dependencies graph for $plugin: $apiGraph")
      val context = runVerifier(graph, plugin, pluginClassesLocations)
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

  /**
   * Selects the plugin's classes that are referenced by the plugin itself and its dependencies.
   * The additional classes (such as those defined by compileServer.plugin extension point) are not selected here.
   */
  private fun IdePluginClassesLocations.constructMainPluginResolver(): Resolver = UnionResolver.create(
      IdePluginClassesFinder.MAIN_CLASSES_KEYS.mapNotNull { this.getResolver(it) }
  )

  private fun runVerifier(graph: DirectedGraph<DepVertex, DepEdge>, plugin: IdePlugin, pluginClassesLocations: IdePluginClassesLocations): VerificationContext {
    val dependenciesResolver = UnionResolver.create(getDependenciesClassesResolvers(graph))
    val checkClasses = MainClassesSelector().getClassesForCheck(pluginClassesLocations)
    val mainPluginResolver = pluginClassesLocations.constructMainPluginResolver()
    //don't close this classLoader because it consists of client resolvers.
    val classLoader = CacheResolver(getVerificationClassLoader(dependenciesResolver, mainPluginResolver))
    return BytecodeVerifier(params, plugin, classLoader, ideDescriptor.ideVersion).verify(checkClasses)
  }

  /**
   * Specifies the order of the classes resolution:
   * 1) firstly a class is searched among classes of the plugin
   * 2) if not found, among the classes of the used JDK
   * 3) if not found, among the libraries of the checked IDE
   * 4) if not found, among the classes of the plugin dependencies' classes
   * 5) if not found, it is finally searched in the external classes specified in the verification arguments.
   */
  private fun getVerificationClassLoader(dependenciesResolver: Resolver, mainPluginResolver: Resolver) = UnionResolver.create(
      listOf(mainPluginResolver, runtimeResolver, ideDescriptor.ideResolver, dependenciesResolver, params.externalClassPath)
  )

  private fun getDependenciesClassesResolvers(graph: DirectedGraph<DepVertex, DepEdge>): List<Resolver> =
      graph.vertexSet()
          .mapNotNull { getClassLocationsByResult(it.resolveResult) }
          .map { it.constructMainPluginResolver() }

  private fun getClassLocationsByResult(result: DependencyResolver.Result): IdePluginClassesLocations? = when (result) {
    is DependencyResolver.Result.FoundReady -> result.pluginClassesLocations
    is DependencyResolver.Result.CreatedResolver -> result.pluginClassesLocations
    is DependencyResolver.Result.Downloaded -> result.pluginClassesLocations
    is DependencyResolver.Result.ProblematicDependency -> null
    is DependencyResolver.Result.NotFound -> null
    is DependencyResolver.Result.FailedToDownload -> null
    DependencyResolver.Result.Skip -> null
  }

  private fun getAppropriateVerdict(context: VerificationContext, dependenciesGraph: DependenciesGraph): Verdict {
    val missingDependencies = dependenciesGraph.start.missingDependencies
    if (missingDependencies.isNotEmpty()) {
      return Verdict.MissingDependencies(dependenciesGraph, context.problems, context.warnings)
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