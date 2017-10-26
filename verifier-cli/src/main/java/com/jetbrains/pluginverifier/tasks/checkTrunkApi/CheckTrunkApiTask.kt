package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.BundledPluginDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.LocalRepositoryDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.RepositoryDependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.repository.LastCompatibleSelector
import com.jetbrains.pluginverifier.dependencies.resolution.repository.LastSelector
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeParams
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeResult
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeTask

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams,
                        private val pluginRepository: PluginRepository,
                        private val pluginDetailsProvider: PluginDetailsProvider) : Task() {

  override fun execute(verificationReportage: VerificationReportage): CheckTrunkApiResult {
    val releaseResults = checkIde(parameters.releaseIde, parameters.pluginsToCheck, ReleaseFinder(), verificationReportage)
    val trunkResults = checkIde(parameters.trunkIde, parameters.pluginsToCheck, TrunkFinder(), verificationReportage)

    return CheckTrunkApiResult(trunkResults, releaseResults)
  }

  private fun checkIde(ideDescriptor: IdeDescriptor,
                       pluginsToCheck: List<PluginCoordinate>,
                       dependencyFinder: DependencyFinder,
                       progress: VerificationReportage): CheckIdeResult {
    val excludedPlugins = IdeResourceUtil.getBrokenPluginsListedInIde(ideDescriptor.ide) ?: emptyList()
    val checkIdeParams = CheckIdeParams(ideDescriptor,
        parameters.jdkDescriptor,
        pluginsToCheck,
        excludedPlugins,
        emptyList(),
        EmptyResolver,
        parameters.externalClassesPrefixes,
        parameters.problemsFilters,
        dependencyFinder
    )
    return CheckIdeTask(checkIdeParams, pluginRepository, pluginDetailsProvider).execute(progress)
  }

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