package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.logging.PluginLogger
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.plugin.create
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.warnings.Warning
import org.jgrapht.DirectedGraph
import java.io.File
import java.util.concurrent.Callable

class Verifier(private val pluginCoordinate: PluginCoordinate,
               private val ideDescriptor: IdeDescriptor,
               private val runtimeResolver: Resolver,
               private val params: VerifierParams,
               private val pluginCreator: PluginCreator,
               private val pluginLogger: PluginLogger) : Callable<Result> {

  companion object {
    private val classesSelectors = listOf(MainClassesSelector(), ExternalBuildClassesSelector())
  }

  override fun call(): Result {
    pluginLogger.started()
    try {
      return createPluginAndDoVerification()
    } finally {
      pluginLogger.finished()
    }
  }

  private fun createPluginAndDoVerification(): Result {
    val createPluginResult = pluginCoordinate.create(pluginCreator)
    return createPluginResult.use {
      val (pluginInfo, verdict) = calculatePluginInfoAndVerdict(createPluginResult)
      Result(pluginInfo, ideDescriptor.ideVersion, verdict)
    }
  }

  private fun calculatePluginInfoAndVerdict(createPluginResult: CreatePluginResult) = when (createPluginResult) {
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
      val (depGraph, start) = graphBuilder.build(creationOk.plugin, creationOk.pluginClassesLocations)
      val apiGraph = DepGraph2ApiGraphConverter.convert(depGraph, start)
      pluginLogger.logDependencyGraph(apiGraph)

      //don't close this classLoader because it contains the client's resolvers.
      val classLoader = createClassLoader(pluginClassesLocations, depGraph)
      val checkClasses = getClassesForCheck(pluginClassesLocations)
      val context = runVerification(plugin, classLoader, checkClasses)
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

  private fun getClassesForCheck(pluginClassesLocations: IdePluginClassesLocations): Set<String> =
      classesSelectors.flatMapTo(hashSetOf()) { it.getClassesForCheck(pluginClassesLocations) }

  private fun createClassLoader(pluginClassesLocations: IdePluginClassesLocations, graph: DirectedGraph<DepVertex, DepEdge>): Resolver {
    val pluginResolver = pluginClassesLocations.createPluginClassLoader()
    val dependenciesResolver = UnionResolver.create(getDependenciesClassesResolvers(graph))
    return CacheResolver(getVerificationClassLoader(dependenciesResolver, pluginResolver))
  }

  private fun IdePluginClassesLocations.createPluginClassLoader(): Resolver {
    val selectedClassLoaders = classesSelectors.map { it.getClassLoader(this) }
    return UnionResolver.create(selectedClassLoaders)
  }

  private fun runVerification(plugin: IdePlugin, classLoader: Resolver, checkClasses: Set<String>): VerificationContext =
      BytecodeVerifier(params, plugin, classLoader, ideDescriptor.ideVersion).verify(checkClasses)

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
          .map { it.createPluginClassLoader() }

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
    val problems = context.problems
    val warnings = context.warnings
    if (missingDependencies.isNotEmpty()) {
      return Verdict.MissingDependencies(dependenciesGraph, problems, warnings)
    }

    if (problems.isNotEmpty()) {
      return Verdict.Problems(problems, dependenciesGraph, warnings)
    }

    if (warnings.isNotEmpty()) {
      return Verdict.Warnings(warnings, dependenciesGraph)
    }

    return Verdict.OK(dependenciesGraph)
  }

}