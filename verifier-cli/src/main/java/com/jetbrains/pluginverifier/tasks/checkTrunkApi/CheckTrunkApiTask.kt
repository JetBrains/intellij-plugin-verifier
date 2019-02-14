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
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClsResolverProvider

/**
 * The 'check-trunk-api' task that runs the verification of a trunk and a release IDEs and reports the new API breakages.
 */
class CheckTrunkApiTask(
    private val parameters: CheckTrunkApiParams,
    private val pluginDetailsCache: PluginDetailsCache,
    pluginRepository: PluginRepository
) : Task {

  override fun execute(
      reportage: Reportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): NewProblemsResult {
    with(parameters) {
      val releaseFinder = createDependencyFinder(parameters.releaseIde.ide, parameters.releaseLocalPluginsRepository)
      val trunkFinder = createDependencyFinder(parameters.trunkIde.ide, parameters.trunkLocalPluginsRepository)

      val releaseTarget = VerificationTarget.Ide(releaseIde.ideVersion)
      val trunkTarget = VerificationTarget.Ide(trunkIde.ideVersion)

      val releaseResolverProvider = DefaultClsResolverProvider(
          releaseFinder,
          jdkDescriptorCache,
          parameters.jdkPath,
          releaseIde,
          parameters.externalClassesPackageFilter
      )
      val trunkResolverProvider = DefaultClsResolverProvider(
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

  private val releaseCompatibleFinder = RepositoryDependencyFinder(
      pluginRepository,
      LastCompatibleVersionSelector(parameters.releaseIde.ideVersion),
      pluginDetailsCache
  )

  private fun createDependencyFinder(ide: Ide, localPluginRepository: PluginRepository): DependencyFinder {
    val bundledFinder = BundledPluginDependencyFinder(ide, pluginDetailsCache)
    val localRepositoryDependencyFinder = RepositoryDependencyFinder(localPluginRepository, LastVersionSelector(), pluginDetailsCache)
    val findersChain = listOf(bundledFinder, localRepositoryDependencyFinder, releaseCompatibleFinder)
    return ChainDependencyFinder(findersChain)
  }

}