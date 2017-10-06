package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.dependencies.graph.DepEdge
import com.jetbrains.pluginverifier.dependencies.graph.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.graph.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.graph.DepVertex
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.progress.DefaultProgressIndicator
import com.jetbrains.pluginverifier.progress.ProgressIndicator
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import java.io.File
import java.util.concurrent.Callable

class PluginVerifier(private val pluginCoordinate: PluginCoordinate,
                     private val ideDescriptor: IdeDescriptor,
                     private val runtimeResolver: Resolver,
                     private val verifierParameters: VerifierParameters,
                     private val pluginDetailsProvider: PluginDetailsProvider,
                     private val pluginVerificationReportage: PluginVerificationReportage) : Callable<Result> {

  companion object {
    private val classesSelectors = listOf(MainClassesSelector(), ExternalBuildClassesSelector())
  }

  override fun call(): Result {
    pluginVerificationReportage.logVerificationStarted()
    try {
      return createPluginAndDoVerification()
    } finally {
      pluginVerificationReportage.logVerificationFinished()
    }
  }

  private fun createPluginAndDoVerification(): Result {
    val pluginDetails = pluginDetailsProvider.fetchPluginDetails(pluginCoordinate)
    return pluginDetails.use {
      val pluginInfo = pluginDetails.toPluginInfo()
      val verdict = pluginDetails.calculateVerdict()
      Result(pluginInfo, ideDescriptor.ideVersion, verdict)
    }
  }

  private fun PluginDetails.toPluginInfo(): PluginInfo = when (this) {
    is PluginDetails.BadPlugin -> pluginCoordinate.toPluginInfo()
    is PluginDetails.NotFound -> pluginCoordinate.toPluginInfo()
    is PluginDetails.FailedToDownload -> pluginCoordinate.toPluginInfo()
    is PluginDetails.ByFileLock -> plugin.getPluginInfo(pluginCoordinate)
    is PluginDetails.FoundOpenPluginAndClasses -> plugin.getPluginInfo(pluginCoordinate)
    is PluginDetails.FoundOpenPluginWithoutClasses -> plugin.getPluginInfo(pluginCoordinate)
  }

  private fun PluginDetails.calculateVerdict() = when (this) {
    is PluginDetails.BadPlugin -> Verdict.Bad(pluginErrorsAndWarnings)
    is PluginDetails.NotFound -> Verdict.NotFound(reason)
    is PluginDetails.FailedToDownload -> Verdict.FailedToDownload(reason)
    is PluginDetails.ByFileLock,
    is PluginDetails.FoundOpenPluginAndClasses,
    is PluginDetails.FoundOpenPluginWithoutClasses -> calculateVerdict(plugin!!, warnings, pluginClassesLocations)
  }

  private fun PluginCoordinate.toPluginInfo(): PluginInfo = when (this) {
    is PluginCoordinate.ByUpdateInfo -> updateInfo
    is PluginCoordinate.ByFile -> guessPluginIdAndVersion(pluginFile)
  }

  private fun guessPluginIdAndVersion(file: File): PluginIdAndVersion {
    val name = file.nameWithoutExtension
    val version = name.substringAfterLast('-')
    return PluginIdAndVersion(name.substringBeforeLast('-'), version)
  }

  private fun IdePlugin.getPluginInfo(pluginCoordinate: PluginCoordinate): PluginInfo =
      (pluginCoordinate as? PluginCoordinate.ByUpdateInfo)?.updateInfo ?: PluginIdAndVersion(pluginId!!, pluginVersion!!)

  private fun calculateVerdict(plugin: IdePlugin,
                               pluginWarnings: List<PluginProblem>?,
                               pluginClassesLocations: IdePluginClassesLocations?): Verdict {
    val resultHolder = VerificationResultHolder(plugin, ideDescriptor.ideVersion, verifierParameters.problemFilters, pluginVerificationReportage)
    if (pluginWarnings != null) {
      resultHolder.addPluginWarnings(pluginWarnings)
    }
    if (pluginClassesLocations != null) {
      runVerification(plugin, pluginClassesLocations, resultHolder)
    }
    return resultHolder.toVerdict()
  }

  private fun runVerification(plugin: IdePlugin,
                              pluginClassesLocations: IdePluginClassesLocations,
                              resultHolder: VerificationResultHolder) {
    val depGraph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)
    try {
      val start = DepVertex(plugin.pluginId!!, PluginDetails.FoundOpenPluginWithoutClasses(plugin))
      DepGraphBuilder(verifierParameters.dependencyFinder).fillDependenciesGraph(start, depGraph)

      val apiGraph = DepGraph2ApiGraphConverter().convert(depGraph, start)
      resultHolder.setDependenciesGraph(apiGraph)

      val pluginResolver = pluginClassesLocations.createPluginClassLoader()
      val dependenciesResolver = depGraph.toDependenciesClassesResolver()
      //don't close this classLoader because it contains the client's resolvers.
      val classLoader = createClassLoader(pluginResolver, dependenciesResolver)
      val checkClasses = getClassesForCheck(pluginClassesLocations)

      val verificationContext = VerificationContext(classLoader, resultHolder, verifierParameters.externalClassesPrefixes)
      val progressIndicator = object : DefaultProgressIndicator() {
        override fun setProgress(value: Double) {
          super.setProgress(value)
          pluginVerificationReportage.logProgress(value)
        }
      }
      runVerification(verificationContext, checkClasses, progressIndicator)
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

  private fun runVerification(verificationContext: VerificationContext, checkClasses: Set<String>, progressIndicator: ProgressIndicator) {
    BytecodeVerifier().verify(checkClasses, verificationContext, progressIndicator)
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

  private fun VerificationResultHolder.toVerdict(): Verdict {
    val dependenciesGraph = getDependenciesGraph()
    if (dependenciesGraph.start.missingDependencies.isNotEmpty()) {
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