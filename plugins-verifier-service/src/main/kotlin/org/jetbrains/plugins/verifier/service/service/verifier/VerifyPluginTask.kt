package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.dependencies.resolution.createIdeBundledOrPluginRepositoryDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task
import java.nio.file.Path

/**
 * Task that performs [scheduledVerification].
 */
class VerifyPluginTask(
    private val scheduledVerification: ScheduledVerification,
    private val jdkPath: Path,
    private val pluginDetailsCache: PluginDetailsCache,
    private val ideDescriptorsCache: IdeDescriptorsCache,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val pluginRepository: PluginRepository,
    private val problemsFilters: List<ProblemsFilter>
) : Task<PluginVerificationResult>("Check ${scheduledVerification.ideVersion} against ${scheduledVerification.updateInfo}", "VerifyPlugin"),
    Comparable<VerifyPluginTask> {

  override fun execute(progress: ProgressIndicator): PluginVerificationResult {
    val cacheEntry = ideDescriptorsCache.getIdeDescriptorCacheEntry(scheduledVerification.ideVersion)
    return cacheEntry.use {
      when (cacheEntry) {
        is IdeDescriptorsCache.Result.Found -> {
          val ideDescriptor = cacheEntry.ideDescriptor
          checkPluginWithIde(ideDescriptor)
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

  private fun checkPluginWithIde(ideDescriptor: IdeDescriptor): PluginVerificationResult {
    val dependencyFinder = createIdeBundledOrPluginRepositoryDependencyFinder(
        ideDescriptor.ide,
        pluginRepository,
        pluginDetailsCache
    )

    return PluginVerifier(
        scheduledVerification.updateInfo,
        PluginVerificationTarget.IDE(ideDescriptor.ide),
        problemsFilters,
        pluginDetailsCache,
        DefaultClassResolverProvider(
            dependencyFinder,
            jdkDescriptorsCache,
            jdkPath,
            ideDescriptor,
            DefaultPackageFilter(emptyList())
        ),
        listOf(DynamicallyLoadedFilter())
    ).loadPluginAndVerify()
  }

  /**
   * Comparison result is used by the task manager
   * to order tasks execution.
   *
   * 1) Newer plugins first.
   * 2) Manually scheduled verifications first.
   */
  override fun compareTo(other: VerifyPluginTask): Int {
    val updateIdCmp = other.scheduledVerification.updateInfo.updateId.compareTo(scheduledVerification.updateInfo.updateId)
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