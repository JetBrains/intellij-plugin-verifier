package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.resolution.*
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

/**
 * The 'check-plugin' task that verifies
 * each plugin from the [CheckPluginParams.pluginsSet]
 * against each IDE from the [CheckPluginParams.ideDescriptors].
 *
 * If one verified plugins depends on
 * another verified plugin then the [dependency resolution] [DependencyFinder]
 * prefers the verified plugin to a plugin from the [PluginRepository].
 */
class CheckPluginTask(private val parameters: CheckPluginParams, private val pluginRepository: PluginRepository) : Task {

  /**
   * Creates the [DependencyFinder] that firstly tries to resolve the dependency among the verified plugins.
   *
   * The 'check-plugin' task searches for dependencies among the verified plugins:
   * suppose plugins A and B are verified simultaneously and A depends on B.
   * Then B must be resolved to the local plugin when the A is verified.
   */
  private fun createDependencyFinder(ideDescriptor: IdeDescriptor, pluginDetailsCache: PluginDetailsCache): DependencyFinder {
    val localFinder = RepositoryDependencyFinder(parameters.pluginsSet.localRepository, LastVersionSelector(), pluginDetailsCache)
    val ideDependencyFinder = createIdeBundledOrPluginRepositoryDependencyFinder(ideDescriptor.ide, pluginRepository, pluginDetailsCache)
    return CompositeDependencyFinder(listOf(localFinder, ideDependencyFinder))
  }

  override fun execute(
      reportage: Reportage,
      verifierExecutor: VerifierExecutor,
      jdkDescriptorCache: JdkDescriptorsCache,
      pluginDetailsCache: PluginDetailsCache
  ): CheckPluginResult {
    with(parameters) {
      val tasks = ideDescriptors.flatMap { ideDescriptor ->
        val dependencyFinder = createDependencyFinder(ideDescriptor, pluginDetailsCache)
        pluginsSet.pluginsToCheck.map {
          PluginVerifier(
              it,
              reportage,
              problemsFilters,
              pluginDetailsCache,
              DefaultClassResolverProvider(
                  dependencyFinder,
                  jdkDescriptorCache,
                  jdkPath,
                  ideDescriptor,
                  externalClassesPackageFilter
              ),
              VerificationTarget.Ide(ideDescriptor.ideVersion),
              ideDescriptor.brokenPlugins,
              listOf(DynamicallyLoadedFilter())
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
