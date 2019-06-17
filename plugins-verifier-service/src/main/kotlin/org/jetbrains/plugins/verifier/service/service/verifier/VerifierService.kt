package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.network.ServerUnavailable503Exception
import com.jetbrains.pluginverifier.parameters.filtering.IgnoredProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.nio.file.Path
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
    private val jdkPath: Path,
    private val verificationResultsFilter: VerificationResultFilter,
    private val pluginRepository: PluginRepository,
    private val serviceDAO: ServiceDAO
) : BaseService("VerifierService", 0, Settings.VERIFIER_SERVICE_SCHEDULER_PERIOD_SECONDS.getAsLong(), TimeUnit.SECONDS, taskManager) {

  private val scheduledVerifications = linkedMapOf<ScheduledVerification, TaskDescriptor>()

  private val lastVerifiedDate = hashMapOf<ScheduledVerification, Instant>()

  private val reportage = object : Reportage {
    override fun createPluginReporters(
        pluginInfo: PluginInfo,
        verificationTarget: VerificationTarget
    ) = Reporters()

    override fun logVerificationStage(stageMessage: String) = Unit

    override fun logPluginVerificationIgnored(
        pluginInfo: PluginInfo,
        verificationTarget: VerificationTarget,
        reason: String
    ) = Unit

    override fun close() = Unit
  }

  private val verifierExecutor = VerifierExecutor(Settings.TASK_MANAGER_CONCURRENCY.getAsInt(), reportage)

  override fun doServe() {
    val allScheduledVerifications = try {
      verifierServiceProtocol.requestScheduledVerifications()
    } catch (e: ServerUnavailable503Exception) {
      logger.info("Do not schedule new verifications and pause the service because the Marketplace is currently unavailable")
      pauseVerification()
      return
    }

    val now = Instant.now()
    val verifications = allScheduledVerifications
        .filter { it.shouldVerify(now) }
        .sortedByDescending { it.updateInfo.updateId }
    logger.info("There are ${verifications.size} pending verifications")
    verifications.forEach { scheduleVerification(it, now) }
  }

  private fun ScheduledVerification.shouldVerify(now: Instant) =
      this !in scheduledVerifications
          && !isCheckedRecently(this, now)
          && verificationResultsFilter.shouldStartVerification(this, now)

  private fun isCheckedRecently(scheduledVerification: ScheduledVerification, now: Instant): Boolean {
    val lastTime = lastVerifiedDate[scheduledVerification] ?: Instant.EPOCH
    return lastTime.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
  }

  private fun scheduleVerification(scheduledVerification: ScheduledVerification, now: Instant) {
    lastVerifiedDate[scheduledVerification] = now

    val ignoreConditions = serviceDAO.ignoreConditions.toList()
    val ignoredProblemsFilter = IgnoredProblemsFilter(ignoreConditions)
    val ignoreProblemsFilters = listOf(ignoredProblemsFilter)

    val task = VerifyPluginTask(
        scheduledVerification,
        verifierExecutor,
        jdkPath,
        pluginDetailsCache,
        ideDescriptorsCache,
        jdkDescriptorsCache,
        pluginRepository,
        ignoreProblemsFilters,
        reportage
    )

    val taskDescriptor = taskManager.enqueue(
        task,
        { taskResult, taskDescriptor -> taskResult.onSuccess(taskDescriptor, scheduledVerification) },
        { error, _ -> onError(scheduledVerification, error) },
        { onCompletion(scheduledVerification) }
    )
    logger.info("Schedule verification $scheduledVerification with task #${taskDescriptor.taskId}")
    scheduledVerifications[scheduledVerification] = taskDescriptor
  }

  @Synchronized
  private fun onCompletion(scheduledVerification: ScheduledVerification) {
    scheduledVerifications.remove(scheduledVerification)
  }

  @Synchronized
  private fun onError(scheduledVerification: ScheduledVerification, error: Throwable) {
    logger.error("Verification failed $scheduledVerification", error)
  }

  /**
   * Temporarily pause verifications because the Marketplace
   * cannot process its results at the moment.
   *
   * Cancel all the scheduled verifications in order to avoid unnecessary work.
   */
  @Synchronized
  private fun pauseVerification() {
    for ((scheduledVerification, taskDescriptor) in scheduledVerifications.entries) {
      logger.info("Cancel verification $scheduledVerification")
      taskManager.cancel(taskDescriptor)
    }
    scheduledVerifications.clear()
  }

  //Do not synchronize: results sending is performed from background threads.
  private fun VerificationResult.onSuccess(taskDescriptor: TaskDescriptor, scheduledVerification: ScheduledVerification) {
    logVerificationResult(scheduledVerification)
    if (verificationResultsFilter.shouldSendVerificationResult(this, taskDescriptor.endTime!!, scheduledVerification)) {
      try {
        verifierServiceProtocol.sendVerificationResult(this, scheduledVerification.updateInfo)
        logger.info("Verification result has been successfully sent for $scheduledVerification")
      } catch (e: ServerUnavailable503Exception) {
        logger.info(
            "Marketplace $pluginRepository is currently unavailable (HTTP 503). " +
                "Stop all the scheduled verification tasks."
        )
        pauseVerification()
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        logger.error("Unable to send verification result for $plugin", e)
      }
    } else {
      logger.info("Verification result for $plugin against $verificationTarget has been ignored")
    }
  }

  private fun VerificationResult.logVerificationResult(scheduledVerification: ScheduledVerification) {
    val message = "Finished verification $scheduledVerification: $verificationVerdict"
    if (this is VerificationResult.FailedToDownload) {
      logger.info(message, failedToDownloadError!!)
    } else {
      logger.info(message)
    }
  }

  override fun onStop() {
    verifierExecutor.close()
  }
}