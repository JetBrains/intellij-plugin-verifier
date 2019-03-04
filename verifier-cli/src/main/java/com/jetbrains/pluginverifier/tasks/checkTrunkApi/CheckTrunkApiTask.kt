package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.resolution.*
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.common.NewProblemsResult
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClassResolverProvider

/**
 * The 'check-trunk-api' task that runs the verification of a trunk and a release IDEs and reports the new API breakages.
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams, private val pluginRepository: PluginRepository) : Task {

  override fun execute(
      reportage: Reportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): NewProblemsResult {
    with(parameters) {
      val releaseFinder = createDependencyFinder(parameters.releaseIde.ide, parameters.releaseLocalPluginsRepository, pluginDetailsCache)
      val trunkFinder = createDependencyFinder(parameters.trunkIde.ide, parameters.trunkLocalPluginsRepository, pluginDetailsCache)

      val releaseTarget = VerificationTarget.Ide(releaseIde.ideVersion)
      val trunkTarget = VerificationTarget.Ide(trunkIde.ideVersion)

      val releaseResolverProvider = DefaultClassResolverProvider(
          releaseFinder,
          jdkDescriptorCache,
          parameters.jdkPath,
          releaseIde,
          parameters.externalClassesPackageFilter
      )
      val trunkResolverProvider = DefaultClassResolverProvider(
          trunkFinder,
          jdkDescriptorCache,
          parameters.jdkPath,
          trunkIde,
          parameters.externalClassesPackageFilter
      )

      val tasks = arrayListOf<PluginVerifier>()

      for (pluginInfo in pluginsSet.pluginsToCheck) {
        tasks.add(
            PluginVerifier(
                pluginInfo,
                reportage,
                parameters.problemsFilters,
                false,
                pluginDetailsCache,
                releaseResolverProvider,
                releaseTarget,
                releaseIde.brokenPlugins
            )
        )

        tasks.add(
            PluginVerifier(
                pluginInfo,
                reportage,
                parameters.problemsFilters,
                false,
                pluginDetailsCache,
                trunkResolverProvider,
                trunkTarget,
                trunkIde.brokenPlugins
            )
        )
      }

      val results = verifierExecutor.verify(tasks)

      return NewProblemsResult.create(
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

    return ChainDependencyFinder(
        listOf(
            bundledFinder,
            localRepositoryDependencyFinder,
            releaseDependencyFinder
        )
    )
  }

}