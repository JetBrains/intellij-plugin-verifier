package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.*
import com.jetbrains.pluginverifier.dependencies.resolution.*
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsVerificationResults
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

/**
 * The 'check-trunk-api' task that runs the verification of a trunk and a release IDEs and reports the new API breakages.
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams, private val pluginRepository: PluginRepository) : Task {

  override fun execute(
      reportage: PluginVerificationReportage,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): TwoTargetsVerificationResults {
    with(parameters) {
      val releaseFinder = createDependencyFinder(releaseIde.ide, releaseLocalPluginsRepository, pluginDetailsCache)
      val trunkFinder = createDependencyFinder(trunkIde.ide, trunkLocalPluginsRepository, pluginDetailsCache)

      val releaseTarget = PluginVerificationTarget.IDE(releaseIde.ide)
      val trunkTarget = PluginVerificationTarget.IDE(trunkIde.ide)

      val releaseResolverProvider = DefaultClassResolverProvider(
          releaseFinder,
          jdkDescriptorCache,
          jdkPath,
          releaseIde,
          externalClassesPackageFilter
      )
      val trunkResolverProvider = DefaultClassResolverProvider(
          trunkFinder,
          jdkDescriptorCache,
          jdkPath,
          trunkIde,
          externalClassesPackageFilter
      )

      val verifiers = arrayListOf<PluginVerifier>()

      val classFilters = listOf(DynamicallyLoadedFilter())
      for (pluginInfo in releasePluginsSet.pluginsToCheck) {
        verifiers += PluginVerifier(
            pluginInfo,
            releaseTarget,
            problemsFilters,
            pluginDetailsCache,
            releaseResolverProvider,
            classFilters
        )
      }

      for (pluginInfo in trunkPluginsSet.pluginsToCheck) {
        verifiers += PluginVerifier(
            pluginInfo,
            trunkTarget,
            problemsFilters,
            pluginDetailsCache,
            trunkResolverProvider,
            classFilters
        )
      }

      /*
       * Sort verification tasks to increase chances that two verifications of the same plugin
       * would be executed shortly, and therefore caches, such as plugin details cache, would be warmed-up.
       */
      val sortedVerifiers = verifiers.sortedBy { it.plugin.pluginId }
      val results = runSeveralVerifiers(reportage, sortedVerifiers)

      return TwoTargetsVerificationResults(
          releaseTarget,
          results.filter { it.verificationTarget == releaseTarget },
          trunkTarget,
          results.filter { it.verificationTarget == trunkTarget }
      )
    }
  }

  /**
   * Creates [DependencyFinder] that searches dependencies using the following order:
   * 1) Bundled with [ide]
   * 2) Available in the local repository [localPluginRepository].
   * 3) Compatible with the **release** IDE
   */
  private fun createDependencyFinder(
      ide: Ide,
      localPluginRepository: PluginRepository,
      pluginDetailsCache: PluginDetailsCache
  ): DependencyFinder {
    val bundledFinder = BundledPluginDependencyFinder(ide, pluginDetailsCache)

    val localRepositoryDependencyFinder = RepositoryDependencyFinder(
        localPluginRepository,
        LastVersionSelector(),
        pluginDetailsCache
    )

    val releaseDependencyFinder = RepositoryDependencyFinder(
        pluginRepository,
        LastCompatibleVersionSelector(parameters.releaseIde.ideVersion),
        pluginDetailsCache
    )

    return CompositeDependencyFinder(
        listOf(
            bundledFinder,
            localRepositoryDependencyFinder,
            releaseDependencyFinder
        )
    )
  }

}