package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Verifier service is responsible for plugins bytecode verification using the *plugin verifier* tool.
 *
 * This service periodically accesses the plugin repository, fetches plugins which should be verified,
 * and sends the verification reports.
 *
 * [Plugin verifier integration with the Plugins Repository](https://confluence.jetbrains.com/display/PLREP/plugin-verifier+integration+with+the+plugins.jetbrains.com)
 */
class VerifierService(
    taskManager: TaskManager,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val verifierServiceProtocol: VerifierServiceProtocol,
    private val pluginDetailsCache: PluginDetailsCache,
    private val ideDescriptorsCache: IdeDescriptorsCache,
    private val jdkPath: JdkPath,
    private val verificationResultsFilter: VerificationResultFilter,
    private val pluginRepository: PluginRepository
) : BaseService("VerifierService", 0, 1, TimeUnit.MINUTES, taskManager) {

  private val running = hashSetOf<ScheduledVerification>()

  private val lastVerifiedDate = hashMapOf<ScheduledVerification, Instant>()

  private val verifierExecutor = VerifierExecutor(Settings.TASK_MANAGER_CONCURRENCY.getAsInt())

  override fun doServe() {
    val now = Instant.now()
    val verifications = verifierServiceProtocol.requestScheduledVerifications()
        .filter { it.shouldVerify(now) }
        .sortedByDescending { it.updateInfo.updateId }
    logger.info("There are ${verifications.size} pending verifications")
    verifications.forEach { scheduleVerification(it, now) }
  }

  private fun ScheduledVerification.shouldVerify(now: Instant) =
      this !in running && !isCheckedRecently(this) && !verificationResultsFilter.shouldIgnoreVerification(this, now)

  private fun isCheckedRecently(scheduledVerification: ScheduledVerification): Boolean {
    val lastCheckTime = lastVerifiedDate[scheduledVerification] ?: Instant.EPOCH
    val now = Instant.now()
    return lastCheckTime.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
  }

  private fun scheduleVerification(scheduledVerification: ScheduledVerification, now: Instant) {
    lastVerifiedDate[scheduledVerification] = now
    running.add(scheduledVerification)
    val task = VerifyPluginTask(
        verifierExecutor,
        scheduledVerification.updateInfo,
        scheduledVerification.ideVersion,
        jdkPath,
        pluginDetailsCache,
        ideDescriptorsCache,
        jdkDescriptorsCache,
        pluginRepository
    )

    val taskDescriptor = taskManager.enqueue(
        task,
        { taskResult, taskDescriptor -> taskResult.onSuccess(taskDescriptor, scheduledVerification) },
        { error, _ -> onError(scheduledVerification, error) },
        { _, _ -> },
        { onCompletion(scheduledVerification) }
    )
    logger.info("Verification $scheduledVerification is scheduled with task #${taskDescriptor.taskId}")
  }

  @Synchronized
  private fun onCompletion(pluginAndTarget: ScheduledVerification) {
    running.remove(pluginAndTarget)
  }

  private fun onError(pluginAndTarget: ScheduledVerification, error: Throwable) {
    logger.error("Unable to check $pluginAndTarget", error)
  }

  private fun VerificationResult.onSuccess(taskDescriptor: TaskDescriptor,
                                           scheduledVerification: ScheduledVerification) {
    logger.info("Verified $plugin against $verificationTarget: ${this}")
    val decision = verificationResultsFilter.shouldSendVerificationResult(this, taskDescriptor.endTime!!, scheduledVerification)
    if (decision == VerificationResultFilter.Result.Send) {
      try {
        verifierServiceProtocol.sendVerificationResult(this, scheduledVerification.updateInfo)
      } catch (e: Exception) {
        logger.error("Unable to send verification result for $plugin", e)
      }
    } else if (decision is VerificationResultFilter.Result.Ignore) {
      logger.info("Verification result for $plugin against $verificationTarget has been ignored: ${decision.ignoreReason}")
    }
  }

  override fun onStop() {
    verifierExecutor.close()
  }
}