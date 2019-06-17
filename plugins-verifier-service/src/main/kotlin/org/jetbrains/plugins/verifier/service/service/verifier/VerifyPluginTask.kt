package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.filter.BundledIdeClassesFilter
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task
import java.nio.file.Path

/**
 * Task that performs [scheduledVerification].
 */
class VerifyPluginTask(
    private val scheduledVerification: ScheduledVerification,
    private val verifierExecutor: VerifierExecutor,
    private val jdkPath: Path,
    private val pluginDetailsCache: PluginDetailsCache,
    private val ideDescriptorsCache: IdeDescriptorsCache,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val pluginRepository: PluginRepository,
    private val problemsFilters: List<ProblemsFilter>,
    private val reportage: Reportage
) : Task<VerificationResult>("Check ${scheduledVerification.ideVersion} against ${scheduledVerification.updateInfo}", "VerifyPlugin"),
    Comparable<VerifyPluginTask> {

  override fun execute(progress: ProgressIndicator): VerificationResult {
    val cacheEntry = ideDescriptorsCache.getIdeDescriptorCacheEntry(scheduledVerification.ideVersion)
    return cacheEntry.use {
      when (cacheEntry) {
        is IdeDescriptorsCache.Result.Found -> {
          val ideDescriptor = cacheEntry.ideDescriptor
          checkPluginWithIde(ideDescriptor, reportage)
        }
        is IdeDescriptorsCache.Result.NotFound -> {
          throw IllegalStateException("IDE ${scheduledVerification.ideVersion} is not found: " + cacheEntry.reason)
        }
        is IdeDescriptorsCache.Result.Failed -> {
          throw IllegalStateException("Failed to get ${scheduledVerification.ideVersion}: ${cacheEntry.reason}", cacheEntry.error)
        }
      }
    }
  }

  private fun checkPluginWithIde(ideDescriptor: IdeDescriptor, reportage: Reportage): VerificationResult {
    val dependencyFinder = IdeDependencyFinder(
        ideDescriptor.ide,
        pluginRepository,
        pluginDetailsCache
    )

    val tasks = listOf(
        PluginVerifier(
            scheduledVerification.updateInfo,
            reportage,
            problemsFilters,
            true,
            pluginDetailsCache,
            DefaultClassResolverProvider(
                dependencyFinder,
                jdkDescriptorsCache,
                jdkPath,
                ideDescriptor,
                PackageFilter(emptyList())
            ),
            VerificationTarget.Ide(ideDescriptor.ideVersion),
            ideDescriptor.brokenPlugins,
            listOf(DynamicallyLoadedFilter(), BundledIdeClassesFilter)
        )
    )
    return verifierExecutor
        .verify(tasks)
        .single()
  }

  /**
   * Comparison result is used by the task manager
   * to order tasks execution.
   *
   * 1) Newer plugins first.
   * 2) Manually scheduled verifications first.
   */
  override fun compareTo(other: VerifyPluginTask): Int {
    val updateIdCmp = Integer.compare(
        other.scheduledVerification.updateInfo.updateId,
        scheduledVerification.updateInfo.updateId
    )
    if (scheduledVerification.manually && other.scheduledVerification.manually) {
      return updateIdCmp
    }
    /**
     * Manually scheduled verification has priority.
     */
    if (scheduledVerification.manually) {
      return -1
    }
    if (other.scheduledVerification.manually) {
      return 1
    }
    return updateIdCmp
  }

}