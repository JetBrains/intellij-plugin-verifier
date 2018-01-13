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
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.UnableToReadPluginClassFilesProblem
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.verifiers.BytecodeVerifier
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import java.util.concurrent.Callable

/**
 * The verification worker that:
 * 1) Downloads the plugin file specified by [pluginInfo] and reads its class files.
 * 2) Builds the dependencies graph of the plugin using the provided [dependencyFinder].
 * 3) Runs the [bytecode verification] [BytecodeVerifier]
 * of plugins' classes against classes of the [IDE] [ideDescriptor],
 * classes of the resolved dependencies and classes of the [jdkDescriptor].
 * The [parameters] [verifierParameters] are used to configure the verification.
 * The [pluginVerificationReportage] is used to log the verification steps,
 * progress, and the results.
 * The [pluginDetailsCache] is used to create the [PluginDetails] of the verified
 * plugin and its dependencies.
 */
class PluginVerifier(private val pluginInfo: PluginInfo,
                     private val ideDescriptor: IdeDescriptor,
                     private val dependencyFinder: DependencyFinder,
                     private val jdkDescriptor: JdkDescriptor,
                     private val verifierParameters: VerifierParameters,
                     private val pluginVerificationReportage: PluginVerificationReportage,
                     private val pluginDetailsCache: PluginDetailsCache) : Callable<Result> {

  companion object {

    /**
     * [Selectors] [ClassesSelector] of the plugins' classes
     * that which classes constitute the plugin class loader
     * used for the verification and which classes should be verified.
     */
    private val classesSelectors = listOf(MainClassesSelector(), ExternalBuildClassesSelector())
  }

  override fun call(): Result {
    pluginVerificationReportage.logVerificationStarted()
    try {
      val result = doVerification()
      pluginVerificationReportage.logVerificationFinished(result.verdict.toString())
      return result
    } catch (e: Throwable) {
      pluginVerificationReportage.logVerificationFinished("Failed with exception: ${e.message}")
      throw e
    }
  }

  private fun doVerification() = pluginDetailsCache.getPluginDetails(pluginInfo).use {
    with(it) {
      when (this) {
        is PluginDetailsCache.Result.Provided -> doVerification(pluginDetails, pluginInfo)
        is PluginDetailsCache.Result.InvalidPlugin -> Result(pluginInfo, ideDescriptor.ideVersion, Verdict.Bad(pluginErrors), emptySet())
        is PluginDetailsCache.Result.FileNotFound -> Result(pluginInfo, ideDescriptor.ideVersion, Verdict.NotFound(reason), emptySet())
        is PluginDetailsCache.Result.Failed -> {
          pluginVerificationReportage.logException("Plugin $pluginInfo was not downloaded", error)
          Result(pluginInfo, ideDescriptor.ideVersion, Verdict.NotFound("Plugin $pluginInfo was not downloaded due to ${error.message}"), emptySet())
        }
      }
    }
  }

  private fun doVerification(pluginDetails: PluginDetails, pluginInfo: PluginInfo): Result {
    val resultHolder = VerificationResultHolder(pluginVerificationReportage)

    resultHolder.addPluginWarnings(pluginDetails.pluginWarnings)
    val badResult = runVerification(pluginDetails, resultHolder)
    if (badResult != null) {
      return badResult
    }

    val verdict = resultHolder.getVerdict()
    pluginVerificationReportage.logVerdict(verdict)
    return Result(
        pluginInfo,
        ideDescriptor.ideVersion,
        verdict,
        resultHolder.ignoredProblemsHolder.ignoredProblems
    )
  }

  private fun runVerification(pluginDetails: PluginDetails,
                              resultHolder: VerificationResultHolder): Result? {
    val depGraph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)
    try {
      buildDependenciesGraph(pluginDetails.plugin, depGraph, resultHolder)
      return runVerification(depGraph, resultHolder, pluginDetails)
    } finally {
      /**
       * Deallocate the dependencies' resources.
       */
      depGraph.vertexSet().forEach { it.dependencyResult.closeLogged() }
    }
  }

  private fun buildDependenciesGraph(plugin: IdePlugin,
                                     depGraph: DirectedGraph<DepVertex, DepEdge>,
                                     resultHolder: VerificationResultHolder) {
    val start = DepVertex(plugin.pluginId!!, DependencyFinder.Result.FoundPlugin(plugin))
    DepGraphBuilder(dependencyFinder).buildDependenciesGraph(depGraph, start)

    val apiGraph = DepGraph2ApiGraphConverter().convert(depGraph, start)
    resultHolder.dependenciesGraph = apiGraph
    pluginVerificationReportage.logDependencyGraph(apiGraph)
    resultHolder.addCycleWarningIfExists(apiGraph)
  }

  private fun runVerification(
      depGraph: DirectedGraph<DepVertex, DepEdge>,
      resultHolder: VerificationResultHolder,
      pluginDetails: PluginDetails
  ): Result? {
    val pluginResolver = try {
      pluginDetails.pluginClassesLocations.createPluginClassLoader()
    } catch (e: Exception) {
      pluginVerificationReportage.logException("Unable to read verified plugin $pluginInfo classes", e)
      return Result(pluginInfo, ideDescriptor.ideVersion, Verdict.Bad(listOf(UnableToReadPluginClassFilesProblem(e))), emptySet())
    }

    val dependenciesResolver = depGraph.createDependenciesResolver()
    //don't close this classLoader because it contains the client's resolvers.
    val classLoader = createClassLoader(pluginResolver, dependenciesResolver)
    val checkClasses = getClassesForCheck(pluginDetails.pluginClassesLocations)

    buildVerificationContextAndDoVerification(pluginDetails.plugin, classLoader, resultHolder, checkClasses)
    return null
  }

  private fun buildVerificationContextAndDoVerification(
      plugin: IdePlugin,
      classLoader: Resolver,
      resultHolder: VerificationResultHolder,
      checkClasses: Set<String>
  ) {
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

  private fun getClassesForCheck(pluginClassesLocations: IdePluginClassesLocations) =
      classesSelectors.flatMapTo(hashSetOf()) { it.getClassesForCheck(pluginClassesLocations) }

  private fun createClassLoader(pluginResolver: Resolver, dependenciesResolver: Resolver) =
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
   * 5) if not found, it is finally searched in the external classes specified in the verification parameters.
   */
  private fun getVerificationClassLoader(dependenciesResolver: Resolver, mainPluginResolver: Resolver) = UnionResolver.create(
      listOf(mainPluginResolver, jdkDescriptor.jdkClassesResolver, ideDescriptor.ideResolver, dependenciesResolver, verifierParameters.externalClassPath)
  )

  private fun DirectedGraph<DepVertex, DepEdge>.createDependenciesResolver(): Resolver {
    val dependenciesResolvers = arrayListOf<Resolver>()
    dependenciesResolvers.closeOnException {
      for (depVertex in vertexSet()) {
        val depPluginClassesLocations = depVertex.getIdePluginClassesLocations()
        if (depPluginClassesLocations != null) {
          val pluginResolver = try {
            depPluginClassesLocations.createPluginClassLoader()
          } catch (e: Exception) {
            pluginVerificationReportage.logException("Unable to read classes of dependency ${depVertex.dependencyId}", e)
            continue
          }
          dependenciesResolvers.add(pluginResolver)
        }
      }
    }
    return UnionResolver.create(dependenciesResolvers)
  }

  private fun DepVertex.getIdePluginClassesLocations(): IdePluginClassesLocations? {
    val cacheResult = (dependencyResult as? DependencyFinder.Result.DetailsProvided)?.pluginDetailsCacheResult
    return (cacheResult as? PluginDetailsCache.Result.Provided)?.pluginDetails?.pluginClassesLocations
  }

  private fun VerificationResultHolder.getVerdict(): Verdict {
    if (pluginWarnings.isNotEmpty()) {
      return Verdict.Bad(pluginWarnings)
    }

    val dependenciesGraph = dependenciesGraph!!
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