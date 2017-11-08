package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.dependencies.resolution.BundledPluginDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.LocalRepositoryDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.RepositoryDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.repository.LastCompatibleSelector
import com.jetbrains.pluginverifier.dependencies.resolution.repository.LastSelector
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.toPluginIdAndVersion
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.tasks.Task
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams,
                        private val pluginRepository: PluginRepository,
                        private val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  override fun execute(verificationReportage: VerificationReportage): CheckTrunkApiResult {
    with(parameters) {
      val allBrokenPlugins = (releaseIde.getIdeBrokenListedPlugins() + trunkIde.getIdeBrokenListedPlugins()).toSet()
      val pluginsToCheck = pluginsToCheck.filterNot { it.toPluginIdAndVersion(pluginDetailsProvider) in allBrokenPlugins }
      val executorService = Executors.newFixedThreadPool(2)
      val releaseResults = executorService.submit(Callable { checkIde(releaseIde, pluginsToCheck, ReleaseFinder(), verificationReportage) })
      val trunkResults = executorService.submit(Callable { checkIde(trunkIde, pluginsToCheck, TrunkFinder(), verificationReportage) })
      return CheckTrunkApiResult.create(releaseIde.ideVersion, releaseResults.get(), trunkIde.ideVersion, trunkResults.get())
    }
  }

  private fun checkIde(ideDescriptor: IdeDescriptor,
                       pluginsToCheck: List<PluginCoordinate>,
                       dependencyFinder: DependencyFinder,
                       verificationReportage: VerificationReportage): List<Result> {
    val verifierParams = VerifierParameters(
        parameters.externalClassesPrefixes,
        parameters.problemsFilters,
        EmptyResolver,
        dependencyFinder,
        false
    )
    val tasks = pluginsToCheck.map { it to ideDescriptor }
    return Verification.run(
        verifierParams,
        pluginDetailsProvider,
        tasks,
        verificationReportage,
        parameters.jdkDescriptor
    )
  }

  private fun IdeDescriptor.getIdeBrokenListedPlugins(): List<PluginIdAndVersion> =
      IdeResourceUtil.getBrokenPluginsListedInIde(ide) ?: emptyList()

  private val publicRepositoryReleaseCompatibleFinder = RepositoryDependencyFinder(pluginRepository, LastCompatibleSelector(parameters.releaseIde.ideVersion), pluginDetailsProvider)

  private inner class ReleaseFinder : DependencyFinder {

    private val releaseBundledFinder = BundledPluginDependencyFinder(parameters.releaseIde.ide, pluginDetailsProvider)

    private val releaseLocalRepositoryFinder = parameters.releaseLocalPluginsRepository?.let { LocalRepositoryDependencyFinder(it, pluginDetailsProvider) }

    override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
      val bundledFound = releaseBundledFinder.findPluginDependency(dependency)
      if (bundledFound !is DependencyFinder.Result.NotFound) {
        return bundledFound
      }

      if (dependency.id in parameters.jetBrainsPluginIds) {
        if (releaseLocalRepositoryFinder != null) {
          val locallyFound = releaseLocalRepositoryFinder.findPluginDependency(dependency)
          if (locallyFound !is DependencyFinder.Result.NotFound) {
            return locallyFound
          }
        }
      }

      return publicRepositoryReleaseCompatibleFinder.findPluginDependency(dependency)
    }
  }

  private inner class TrunkFinder : DependencyFinder {

    private val trunkBundledFinder = BundledPluginDependencyFinder(parameters.trunkIde.ide, pluginDetailsProvider)

    private val trunkLocalFinder = parameters.trunkLocalPluginsRepository?.let { LocalRepositoryDependencyFinder(it, pluginDetailsProvider) }

    private val downloadLastUpdateResolver = RepositoryDependencyFinder(pluginRepository, LastSelector(), pluginDetailsProvider)

    override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
      val bundledFound = trunkBundledFinder.findPluginDependency(dependency)
      if (bundledFound !is DependencyFinder.Result.NotFound) {
        return bundledFound
      }

      if (dependency.isModule || dependency.id in parameters.jetBrainsPluginIds) {
        if (trunkLocalFinder != null) {
          val locallyFound = trunkLocalFinder.findPluginDependency(dependency)
          if (locallyFound !is DependencyFinder.Result.NotFound) {
            return locallyFound
          }
        }
        return downloadLastUpdateResolver.findPluginDependency(dependency)
      }

      return publicRepositoryReleaseCompatibleFinder.findPluginDependency(dependency)
    }
  }


}