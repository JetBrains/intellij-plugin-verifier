package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.*
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.parameters.filtering.toPluginIdAndVersion
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
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
                        private val pluginDetailsCache: PluginDetailsCache) : Task {

  override fun execute(verificationReportage: VerificationReportage): CheckTrunkApiResult {
    with(parameters) {
      val allBrokenPlugins = (releaseIde.getIdeBrokenListedPlugins() + trunkIde.getIdeBrokenListedPlugins()).toSet()
      val pluginsToCheck = pluginsToCheck.filterNot { it.toPluginIdAndVersion() in allBrokenPlugins }
      val executorService = Executors.newFixedThreadPool(2)
      try {
        val releaseResults = executorService.submit(Callable { checkIde(releaseIde, pluginsToCheck, ReleaseFinder(), verificationReportage) })
        val trunkResults = executorService.submit(Callable { checkIde(trunkIde, pluginsToCheck, TrunkFinder(), verificationReportage) })
        return CheckTrunkApiResult.create(releaseIde.ideVersion, releaseResults.get(), trunkIde.ideVersion, trunkResults.get())
      } finally {
        executorService.shutdownNow()
      }
    }
  }

  private fun checkIde(ideDescriptor: IdeDescriptor,
                       pluginsToCheck: List<PluginInfo>,
                       dependencyFinder: DependencyFinder,
                       verificationReportage: VerificationReportage): List<Result> {
    val verifierParams = VerifierParameters(
        parameters.externalClassesPrefixes,
        parameters.problemsFilters,
        EmptyResolver,
        false
    )
    val tasks = pluginsToCheck.map { VerifierTask(it, ideDescriptor, dependencyFinder) }
    return Verification.run(
        verifierParams,
        pluginDetailsCache,
        tasks,
        verificationReportage,
        parameters.jdkDescriptor
    )
  }

  private fun IdeDescriptor.getIdeBrokenListedPlugins(): List<PluginIdAndVersion> =
      IdeResourceUtil.getBrokenPluginsListedInIde(ide) ?: emptyList()

  private val publicRepositoryReleaseCompatibleFinder = RepositoryDependencyFinder(pluginRepository, LastCompatibleVersionSelector(parameters.releaseIde.ideVersion), pluginDetailsCache)

  private inner class ReleaseFinder : DependencyFinder {

    private val releaseBundledFinder = BundledPluginDependencyFinder(parameters.releaseIde.ide)

    private val releaseLocalRepositoryFinder = parameters.releaseLocalPluginsRepository?.let { LocalRepositoryDependencyFinder(it, pluginDetailsCache) }

    override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
      val bundledFound = releaseBundledFinder.findPluginDependency(dependency)
      if (bundledFound !is DependencyFinder.Result.NotFound) {
        return bundledFound
      }

      if (dependency.isModule || dependency.id in parameters.jetBrainsPluginIds) {
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

    private val trunkBundledFinder = BundledPluginDependencyFinder(parameters.trunkIde.ide)

    private val trunkLocalFinder = parameters.trunkLocalPluginsRepository?.let { LocalRepositoryDependencyFinder(it, pluginDetailsCache) }

    private val downloadLastUpdateResolver = RepositoryDependencyFinder(pluginRepository, LastVersionSelector(), pluginDetailsCache)

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