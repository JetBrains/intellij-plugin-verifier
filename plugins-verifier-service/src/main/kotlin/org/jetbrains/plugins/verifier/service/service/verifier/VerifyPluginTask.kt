package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClsResolverProvider
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskCancelledException

/**
 * [Task] verifies the [plugin] [updateInfo]
 * against the [ideVersion] in the [verifierExecutor]
 * using the [JDK] [jdkPath].
 */
class VerifyPluginTask(
    private val scheduledVerification: ScheduledVerification,
    private val verifierExecutor: VerifierExecutor,
    private val jdkPath: JdkPath,
    private val pluginDetailsCache: PluginDetailsCache,
    private val ideDescriptorsCache: IdeDescriptorsCache,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val pluginRepository: PluginRepository,
    private val problemsFilters: List<ProblemsFilter>
) : Task<VerificationResult>("Check ${scheduledVerification.ideVersion} against ${scheduledVerification.updateInfo}", "VerifyPlugin"),
    Comparable<VerifyPluginTask> {

  override fun execute(progress: ProgressIndicator): VerificationResult {
    val cacheEntry = ideDescriptorsCache.getIdeDescriptorCacheEntry(scheduledVerification.ideVersion)
    return cacheEntry.use {
      when (cacheEntry) {
        is IdeDescriptorsCache.Result.Found -> {
          val ideDescriptor = cacheEntry.ideDescriptor
          val reportage = createReportage(progress)
          checkPluginWithIde(ideDescriptor, reportage)
        }
        is IdeDescriptorsCache.Result.NotFound -> {
          throw TaskCancelledException("IDE ${scheduledVerification.ideVersion} is not found: " + cacheEntry.reason)
        }
        is IdeDescriptorsCache.Result.Failed -> {
          throw TaskCancelledException("Failed to get ${scheduledVerification.ideVersion}: ${cacheEntry.reason}", cacheEntry.error)
        }
      }
    }
  }

  private fun checkPluginWithIde(
      ideDescriptor: IdeDescriptor,
      reportage: Reportage
  ): VerificationResult {
    val dependencyFinder = IdeDependencyFinder(
        ideDescriptor.ide,
        pluginRepository,
        pluginDetailsCache
    )

    val tasks = listOf(PluginVerifier(
        scheduledVerification.updateInfo,
        reportage,
        problemsFilters,
        true,
        pluginDetailsCache,
        DefaultClsResolverProvider(
            dependencyFinder,
            jdkDescriptorsCache,
            jdkPath,
            ideDescriptor,
            PackageFilter(emptyList())
        ),
        VerificationTarget.Ide(ideDescriptor.ideVersion)
    ))
    return verifierExecutor
        .verify(tasks)
        .single()
  }

  private fun createDelegatingReporter(progress: ProgressIndicator): Reporter<Double> {
    return object : Reporter<Double> {
      override fun report(t: Double) {
        progress.fraction = t
      }

      override fun close() = Unit
    }
  }

  private fun createReportage(progress: ProgressIndicator) =
      object : Reportage {
        override fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget) =
            Reporters(
                progressReporters = listOf(createDelegatingReporter(progress))
            )

        override fun logVerificationStage(stageMessage: String) = Unit

        override fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: VerificationTarget, reason: String) = Unit

        override fun close() = Unit
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