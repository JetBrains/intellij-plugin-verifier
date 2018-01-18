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
import com.jetbrains.pluginverifier.reporting.verification.EmptyPluginVerificationReportage.ideVersion
import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
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
 * classes of the resolved dependencies and classes of the [JDK] [jdkDescriptor].
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
                     private val pluginDetailsCache: PluginDetailsCache) : Callable<VerificationResult> {

  companion object {

    /**
     * [Selectors] [ClassesSelector] of the plugins' classes
     * that constitute the plugin [class loader] [ClassesSelector.getClassLoader]
     * and classes that should be [verified] [ClassesSelector.getClassesForCheck].
     */
    private val classesSelectors = listOf(MainClassesSelector(), ExternalBuildClassesSelector())
  }

  override fun call(): VerificationResult {
    pluginVerificationReportage.logVerificationStarted()
    try {
      val result = doVerification()
      pluginVerificationReportage.logVerificationResult(result)
      pluginVerificationReportage.logVerificationFinished(result.toString())
      return result
    } catch (e: Throwable) {
      pluginVerificationReportage.logVerificationFinished("Failed with exception: ${e.message}")
      throw RuntimeException("Failed to verify $pluginInfo against $ideDescriptor", e)
    }
  }

  private fun doVerification() = pluginDetailsCache.getPluginDetailsCacheEntry(pluginInfo).use {
    with(it) {
      when (this) {
        is PluginDetailsCache.Result.Provided -> doVerification(pluginDetails)
        is PluginDetailsCache.Result.InvalidPlugin -> VerificationResult.InvalidPlugin(
            pluginInfo,
            ideDescriptor.ideVersion,
            emptySet(),
            pluginErrors
                .filter { it.level == PluginProblem.Level.ERROR }
                .map { PluginStructureError(it.message) }
        )
        is PluginDetailsCache.Result.FileNotFound -> VerificationResult.NotFound(pluginInfo, ideDescriptor.ideVersion, emptySet(), reason)
        is PluginDetailsCache.Result.Failed -> {
          pluginVerificationReportage.logException("Plugin $pluginInfo was not downloaded", error)
          VerificationResult.FailedToDownload(pluginInfo, ideDescriptor.ideVersion, emptySet(), "Plugin $pluginInfo was not downloaded due to ${error.message}")
        }
      }
    }
  }

  private fun doVerification(pluginDetails: PluginDetails): VerificationResult {
    val resultHolder = VerificationResultHolder(pluginVerificationReportage)

    resultHolder.addPluginWarnings(pluginDetails.pluginWarnings)
    val badResult = runVerification(pluginDetails, resultHolder)
    if (badResult != null) {
      return badResult
    }

    return resultHolder.getVerificationResult()
  }

  private fun runVerification(pluginDetails: PluginDetails,
                              resultHolder: VerificationResultHolder): VerificationResult? {
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
  ): VerificationResult? {
    val ignoredProblems = resultHolder.ignoredProblemsHolder.ignoredProblems

    fun createInvalidPluginResult(e: Exception) = VerificationResult.InvalidPlugin(
        pluginInfo,
        ideVersion,
        ignoredProblems,
        listOf(PluginStructureError(UnableToReadPluginClassFilesProblem(e).message))
    )

    /**
     * Create the plugin's own classes resolver.
     */
    val pluginResolver = try {
      pluginDetails.pluginClassesLocations.createPluginClassLoader()
    } catch (e: Exception) {
      pluginVerificationReportage.logException("Unable to read classes of the verified plugin $pluginInfo", e)
      return createInvalidPluginResult(e)
    }

    /**
     * Create the dependent plugins' resolvers.
     */
    val dependenciesResolver = depGraph.createDependenciesResolver()

    /**
     * Create the plugin's class loader used during the verification.
     * Don't close this classLoader because it contains the client's resolvers.
     */
    val classLoader = try {
      getVerificationClassLoader(pluginResolver, dependenciesResolver)
    } catch (e: Exception) {
      pluginVerificationReportage.logException("Unable to create the plugin class loader of $pluginInfo", e)
      return createInvalidPluginResult(e)
    }

    /**
     * Select classes for the verification.
     */
    val checkClasses = try {
      getClassesForCheck(pluginDetails.pluginClassesLocations)
    } catch (e: Exception) {
      pluginVerificationReportage.logException("Unable to select classes for check of $pluginInfo", e)
      return createInvalidPluginResult(e)
    }

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

    BytecodeVerifier().verify(checkClasses, verificationContext, progressIndicator)
  }

  private fun getClassesForCheck(pluginClassesLocations: IdePluginClassesLocations) =
      classesSelectors.flatMapTo(hashSetOf()) { it.getClassesForCheck(pluginClassesLocations) }

  private fun IdePluginClassesLocations.createPluginClassLoader() = UnionResolver.create(
      classesSelectors.map { it.getClassLoader(this) }
  )

  /**
   * Specifies the order of the classes resolution:
   * 1) firstly a class is searched among classes of the plugin
   * 2) if not found, among the classes of the used JDK
   * 3) if not found, among the libraries of the checked IDE
   * 4) if not found, among the classes of the plugin dependencies' classes
   * 5) if not found, it is finally searched in the external classes specified in the verification parameters.
   */
  private fun getVerificationClassLoader(mainPluginResolver: Resolver, dependenciesResolver: Resolver) = CacheResolver(
      UnionResolver.create(
          listOf(
              mainPluginResolver,
              jdkDescriptor.jdkClassesResolver,
              ideDescriptor.ideResolver,
              dependenciesResolver,
              verifierParameters.externalClassPath
          )
      ))

  private fun DirectedGraph<DepVertex, DepEdge>.createDependenciesResolver(): Resolver {
    val dependenciesResolvers = arrayListOf<Resolver>()
    dependenciesResolvers.closeOnException {
      for (depVertex in vertexSet()) {
        val classesLocations = depVertex.getIdePluginClassesLocations()
        if (classesLocations != null) {
          val pluginResolver = try {
            classesLocations.createPluginClassLoader()
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

  private fun VerificationResultHolder.getVerificationResult(): VerificationResult {
    val ideVersion = ideDescriptor.ideVersion

    val ignoredProblems = ignoredProblemsHolder.ignoredProblems

    val pluginStructureWarnings = pluginWarnings
        .filter { it.level == PluginProblem.Level.WARNING }
        .mapTo(hashSetOf()) { PluginStructureWarning(it.message) }

    val dependenciesGraph = dependenciesGraph!!

    if (dependenciesGraph.verifiedPlugin.missingDependencies.isNotEmpty()) {
      return VerificationResult.MissingDependencies(
          pluginInfo,
          ideVersion,
          ignoredProblems,
          dependenciesGraph,
          compatibilityProblems,
          pluginStructureWarnings,
          deprecatedUsages
      )
    }

    if (compatibilityProblems.isNotEmpty()) {
      return VerificationResult.Problems(
          pluginInfo,
          ideVersion,
          ignoredProblems,
          compatibilityProblems,
          dependenciesGraph,
          pluginStructureWarnings,
          deprecatedUsages
      )
    }

    if (pluginStructureWarnings.isNotEmpty()) {
      return VerificationResult.Warnings(
          pluginInfo,
          ideVersion,
          ignoredProblems,
          pluginStructureWarnings,
          dependenciesGraph,
          deprecatedUsages
      )
    }

    return VerificationResult.OK(pluginInfo, ideVersion, ignoredProblems, dependenciesGraph, deprecatedUsages)
  }

}