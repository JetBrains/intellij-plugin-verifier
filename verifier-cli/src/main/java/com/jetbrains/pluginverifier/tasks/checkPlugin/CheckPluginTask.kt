package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.resolution.*
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClsResolverProvider

/**
 * The 'check-plugin' [task] [Task] that verifies
 * each plugin from the [CheckPluginParams.pluginsSet]
 * against each IDE from the [CheckPluginParams.ideDescriptors].
 *
 * If one [verified] [CheckPluginParams.pluginsSet] plugin depends on
 * another verified plugin then the [dependency resolution] [DependencyFinder]
 * prefers the verified plugin to a plugin from the [PluginRepository].
 */
class CheckPluginTask(private val parameters: CheckPluginParams,
                      private val pluginRepository: PluginRepository,
                      private val pluginDetailsCache: PluginDetailsCache) : Task {

  /**
   * Creates the [DependencyFinder] that:
   * 1) Resolves the [dependency] [PluginDependency] among the [verified] [CheckPluginParams.pluginsSet] plugins.
   * The 'check-plugin' task searches dependencies among the verified plugins:
   * suppose plugins A and B are verified simultaneously and A depends on B.
   * Then B must be resolved to the local plugin when the plugin A is verified.
   *
   * 2) If not found, resolves the [dependency] [PluginDependency] using the [IdeDependencyFinder].
   */
  private fun createDependencyFinder(ideDescriptor: IdeDescriptor): DependencyFinder {
    val localFinder = RepositoryDependencyFinder(parameters.pluginsSet.localRepository, LastVersionSelector(), pluginDetailsCache)
    val ideDependencyFinder = IdeDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
    return ChainDependencyFinder(listOf(localFinder, ideDependencyFinder))
  }

  override fun execute(
      reportage: Reportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): CheckPluginResult {
    with(parameters) {
      val tasks = ideDescriptors.flatMap { ideDescriptor ->
        val dependencyFinder = createDependencyFinder(ideDescriptor)
        pluginsSet.pluginsToCheck.map {
          PluginVerifier(
              it,
              reportage,
              problemsFilters,
              true,
              pluginDetailsCache,
              DefaultClsResolverProvider(
                  dependencyFinder,
                  jdkDescriptorCache,
                  jdkPath,
                  ideDescriptor,
                  externalClassesPackageFilter
              ),
              VerificationTarget.Ide(ideDescriptor.ideVersion),
              ideDescriptor.brokenPlugins
          )
        }
      }
      val results = verifierExecutor.verify(tasks)
      return CheckPluginResult(
          pluginsSet.invalidPluginFiles,
          results
      )
    }
  }

}
