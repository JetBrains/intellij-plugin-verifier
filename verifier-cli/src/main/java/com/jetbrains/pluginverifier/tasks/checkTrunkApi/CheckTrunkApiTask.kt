package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.resolution.*
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeResourceUtil
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.filtering.toPluginIdAndVersion
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task

/**
 * The 'check-trunk-api' task that runs the verification
 * of a [trunk] [CheckTrunkApiParams.trunkIde] and a [release] [CheckTrunkApiParams.releaseIde] IDEs
 * and reports the new API breakages.
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams,
                        private val pluginRepository: PluginRepository,
                        private val pluginDetailsCache: PluginDetailsCache) : Task {

  override fun execute(verificationReportage: VerificationReportage): CheckTrunkApiResult {
    with(parameters) {
      val allBrokenPlugins = (releaseIde.getIdeBrokenListedPlugins() + trunkIde.getIdeBrokenListedPlugins()).toSet()
      val pluginInfosToCheck = pluginsToCheck.plugins.filterNot { it.toPluginIdAndVersion() in allBrokenPlugins }

      val verifierParameters = VerifierParameters(
          parameters.externalClassesPrefixes,
          parameters.problemsFilters,
          EmptyResolver,
          false
      )

      val verifierTasks = arrayListOf<VerifierTask>()
      pluginInfosToCheck.mapTo(verifierTasks) {
        VerifierTask(it, releaseIde, ReleaseFinder())
      }
      pluginInfosToCheck.mapTo(verifierTasks) {
        VerifierTask(it, trunkIde, TrunkFinder())
      }

      val results = Verification.run(
          verifierParameters,
          pluginDetailsCache,
          verifierTasks,
          verificationReportage,
          jdkDescriptor
      )

      return CheckTrunkApiResult.create(
          releaseIde.ideVersion,
          results.filter { it.ideVersion == releaseIde.ideVersion },
          trunkIde.ideVersion,
          results.filter { it.ideVersion == trunkIde.ideVersion },
          pluginsToCheck.invalidPluginFiles
      )
    }
  }

  private fun IdeDescriptor.getIdeBrokenListedPlugins() =
      IdeResourceUtil.getBrokenPluginsListedInIde(ide).orEmpty()

  private val publicRepositoryReleaseCompatibleFinder = RepositoryDependencyFinder(pluginRepository, LastCompatibleVersionSelector(parameters.releaseIde.ideVersion), pluginDetailsCache)

  /**
   * [DependencyFinder] for the verification of the [release] [CheckTrunkApiParams.releaseIde] IDE that:
   * 1) Resolves a [dependency] [PluginDependency] among the [bundled] [releaseBundledFinder] plugins of the _release_ IDE.
   * 2) If not resolved, if the dependency is a JetBrains-developed plugin,
   * resolves the plugin in the [local] [releaseLocalRepositoryFinder]
   * plugins repository that consists of plugins which were built from the same sources as the _release_ IDE was.
   * 3) Finally, resolves the dependency using the [RepositoryDependencyFinder] of the [pluginRepository].
   */
  private inner class ReleaseFinder : DependencyFinder {

    private val releaseBundledFinder = BundledPluginDependencyFinder(parameters.releaseIde.ide, pluginDetailsCache)

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

  /**
   * [DependencyFinder] for the verification of the [trunk] [CheckTrunkApiParams.trunkIde] IDE that:
   * 1) Resolves a [dependency] [PluginDependency] among the [bundled] [trunkBundledFinder] plugins of the _trunk_ IDE.
   * 2) If not resolved, if the dependency is a JetBrains-developed plugin,
   * resolves the plugin in the [local] [trunkLocalFinder] plugins repository
   * that consists of plugins which were built from the same sources as the _trunk_ IDE was.
   * 3) Finally, resolves the dependency using the [RepositoryDependencyFinder] such that
   * - if the dependency is a JetBrains-developed plugin, the _last_ version of it is requested
   * from the repository.
   * - otherwise, resolves a version of the dependency that is compatible with the _release_ IDE.
   *
   * Note that we consider the IntelliJ API is both the IDE's classes and all the classes of
   * the JetBrains plugins that could be dependencies of third-party plugins.
   */
  private inner class TrunkFinder : DependencyFinder {

    private val trunkBundledFinder = BundledPluginDependencyFinder(parameters.trunkIde.ide, pluginDetailsCache)

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