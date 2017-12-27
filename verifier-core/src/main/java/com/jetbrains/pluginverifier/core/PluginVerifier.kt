package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.dependencies.graph.DepEdge
import com.jetbrains.pluginverifier.dependencies.graph.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.graph.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.graph.DepVertex
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import java.nio.file.Path
import java.util.concurrent.Callable

class PluginVerifier(private val pluginCoordinate: PluginCoordinate,
                     private val ideDescriptor: IdeDescriptor,
                     private val dependencyFinder: DependencyFinder,
                     private val runtimeResolver: Resolver,
                     private val verifierParameters: VerifierParameters,
                     private val pluginDetailsProvider: PluginDetailsProvider,
                     private val pluginVerificationReportage: PluginVerificationReportage) : Callable<Result> {

  companion object {
    private val classesSelectors = listOf(MainClassesSelector(), ExternalBuildClassesSelector())
  }

  override fun call(): Result {
    pluginVerificationReportage.logVerificationStarted()
    return try {
      createPluginAndDoVerification()
    } finally {
      pluginVerificationReportage.logVerificationFinished()
    }
  }

  private fun createPluginAndDoVerification() =
      pluginDetailsProvider.providePluginDetails(pluginCoordinate).use { pluginDetails ->
        val pluginInfo = pluginDetails.toPluginInfo()
        pluginDetails.doVerification(pluginInfo)
      }

  private fun PluginDetails.toPluginInfo(): PluginInfo = when (this) {
    is PluginDetails.BadPlugin -> pluginCoordinate.toPluginInfo()
    is PluginDetails.NotFound -> pluginCoordinate.toPluginInfo()
    is PluginDetails.FailedToDownload -> pluginCoordinate.toPluginInfo()
    is PluginDetails.ByFileLock -> plugin.getPluginInfo(pluginCoordinate)
    is PluginDetails.FoundOpenPluginAndClasses -> plugin.getPluginInfo(pluginCoordinate)
    is PluginDetails.FoundOpenPluginWithoutClasses -> plugin.getPluginInfo(pluginCoordinate)
  }

  private fun PluginDetails.doVerification(pluginInfo: PluginInfo): Result {
    val resultHolder = VerificationResultHolder(pluginVerificationReportage)
    calculateVerificationResults(resultHolder)
    val verdict = resultHolder.getVerdict()
    pluginVerificationReportage.logVerdict(verdict)
    return Result(
        pluginInfo,
        ideDescriptor.ideVersion,
        verdict,
        resultHolder.ignoredProblemsHolder.ignoredProblems
    )
  }

  private fun PluginDetails.calculateVerificationResults(resultHolder: VerificationResultHolder): Any = when (this) {
    is PluginDetails.BadPlugin -> {
      resultHolder.pluginProblems.addAll(pluginErrorsAndWarnings)
    }
    is PluginDetails.NotFound -> {
      resultHolder.notFoundReason = reason
    }
    is PluginDetails.FailedToDownload -> {
      resultHolder.failedToDownloadReason = reason
    }
    is PluginDetails.ByFileLock,
    is PluginDetails.FoundOpenPluginAndClasses,
    is PluginDetails.FoundOpenPluginWithoutClasses -> {
      if (warnings != null) {
        resultHolder.addPluginWarnings(warnings!!)
      }
      runVerification(plugin!!, pluginClassesLocations, resultHolder)
    }
  }

  private fun PluginCoordinate.toPluginInfo(): PluginInfo = when (this) {
    is PluginCoordinate.ByUpdateInfo -> updateInfo
    is PluginCoordinate.ByFile -> guessPluginIdAndVersion(pluginFile)
  }

  private fun guessPluginIdAndVersion(file: Path): PluginIdAndVersion {
    val name = file.nameWithoutExtension
    val version = name.substringAfterLast('-')
    return PluginIdAndVersion(name.substringBeforeLast('-'), version)
  }

  private fun IdePlugin.getPluginInfo(pluginCoordinate: PluginCoordinate): PluginInfo =
      (pluginCoordinate as? PluginCoordinate.ByUpdateInfo)?.updateInfo ?: PluginIdAndVersion(pluginId!!, pluginVersion!!)

  private fun runVerification(plugin: IdePlugin,
                              pluginClassesLocations: IdePluginClassesLocations?,
                              resultHolder: VerificationResultHolder) {
    val depGraph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)
    try {
      val start = DepVertex(plugin.pluginId!!, PluginDetails.FoundOpenPluginWithoutClasses(plugin))
      DepGraphBuilder(dependencyFinder).fillDependenciesGraph(start, depGraph)

      val apiGraph = DepGraph2ApiGraphConverter().convert(depGraph, start)
      resultHolder.setDependenciesGraph(apiGraph)

      if (pluginClassesLocations != null) {
        val pluginResolver = pluginClassesLocations.createPluginClassLoader()
        val dependenciesResolver = depGraph.toDependenciesClassesResolver()
        //don't close this classLoader because it contains the client's resolvers.
        val classLoader = createClassLoader(pluginResolver, dependenciesResolver)
        val checkClasses = getClassesForCheck(pluginClassesLocations)

        val verificationContext = VerificationContext(
            plugin,
            ideDescriptor.ideVersion,
            classLoader,
            ideDescriptor.ideResolver,
            resultHolder,
            verifierParameters.externalClassesPrefixes,
            verifierParameters.findDeprecatedApiUsages,
            verifierParameters.problemFilters
        )
        val progressIndicator = object : Reporter<Double> {
          override fun close() = Unit

          override fun report(t: Double) {
            pluginVerificationReportage.logProgress(t)
          }
        }
        runVerification(verificationContext, checkClasses, progressIndicator)
      }
    } finally {
      depGraph.vertexSet().forEach { it.pluginDetails.closeLogged() }
    }
  }

  private fun getClassesForCheck(pluginClassesLocations: IdePluginClassesLocations): Set<String> =
      classesSelectors.flatMapTo(hashSetOf()) { it.getClassesForCheck(pluginClassesLocations) }

  private fun createClassLoader(pluginResolver: Resolver, dependenciesResolver: Resolver): Resolver =
      CacheResolver(getVerificationClassLoader(dependenciesResolver, pluginResolver))

  private fun IdePluginClassesLocations.createPluginClassLoader(): Resolver {
    val selectedClassLoaders = classesSelectors.map { it.getClassLoader(this) }
    return UnionResolver.create(selectedClassLoaders)
  }

  private fun runVerification(verificationContext: VerificationContext, checkClasses: Set<String>, progressReporter: Reporter<Double>) {
    BytecodeVerifier().verify(checkClasses, verificationContext, progressReporter)
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
      listOf(mainPluginResolver, runtimeResolver, ideDescriptor.ideResolver, dependenciesResolver, verifierParameters.externalClassPath)
  )

  private fun DirectedGraph<DepVertex, DepEdge>.toDependenciesClassesResolver(): Resolver =
      UnionResolver.create(vertexSet()
          .mapNotNull { it.pluginDetails.pluginClassesLocations }
          .map { it.createPluginClassLoader() }
      )

  private fun VerificationResultHolder.getVerdict(): Verdict {
    if (notFoundReason != null) {
      return Verdict.NotFound(notFoundReason!!)
    }

    if (failedToDownloadReason != null) {
      return Verdict.FailedToDownload(failedToDownloadReason!!)
    }

    if (pluginProblems.isNotEmpty()) {
      return Verdict.Bad(pluginProblems)
    }

    val dependenciesGraph = getDependenciesGraph()
    if (dependenciesGraph.start.missingDependencies.isNotEmpty()) {
      return Verdict.MissingDependencies(dependenciesGraph, problems, warnings, deprecatedUsages)
    }

    if (problems.isNotEmpty()) {
      return Verdict.Problems(problems, dependenciesGraph, warnings, deprecatedUsages)
    }

    if (warnings.isNotEmpty()) {
      return Verdict.Warnings(warnings, dependenciesGraph, deprecatedUsages)
    }

    return Verdict.OK(dependenciesGraph, deprecatedUsages)
  }

}