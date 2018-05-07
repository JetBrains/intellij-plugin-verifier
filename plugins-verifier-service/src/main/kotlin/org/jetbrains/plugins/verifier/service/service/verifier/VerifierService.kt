package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskManager
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskStatus
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
    taskManager: ServiceTaskManager,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val verifierServiceProtocol: VerifierServiceProtocol,
    private val ideFilesBank: IdeFilesBank,
    private val pluginDetailsCache: PluginDetailsCache,
    private val ideDescriptorsCache: IdeDescriptorsCache,
    private val jdkPath: JdkPath,
    private val verificationResultsFilter: VerificationResultFilter
) : BaseService("VerifierService", 0, 1, TimeUnit.MINUTES, taskManager) {

  private val inProgress = hashSetOf<PluginAndTarget>()

  private val lastCheckDate = hashMapOf<PluginAndTarget, Instant>()

  private val verifierExecutor = VerifierExecutor(Settings.TASK_MANAGER_CONCURRENCY.getAsInt())

  override fun doServe() {
    val pluginsToCheck = requestPluginsToCheck()
    logger.info("There are ${pluginsToCheck.size} plugins waiting for verification")
    pluginsToCheck.forEach { scheduleVerification(it) }
  }

  private fun isCheckedRecently(pluginAndTarget: PluginAndTarget): Boolean {
    val lastCheckTime = lastCheckDate[pluginAndTarget] ?: Instant.EPOCH
    val now = Instant.now()
    return lastCheckTime.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
  }

  private fun requestPluginsToCheck(): List<PluginAndTarget> =
      ideFilesBank
          .getAvailableIdeVersions()
          .flatMap { ideVersion ->
            verifierServiceProtocol
                .requestUpdatesToCheck(ideVersion)
                .map { PluginAndTarget(it, VerificationTarget.Ide(ideVersion)) }
          }

  private fun scheduleVerification(pluginAndTarget: PluginAndTarget) {
    if (pluginAndTarget in inProgress
        || isCheckedRecently(pluginAndTarget)
        || verificationResultsFilter.ignoredVerifications.containsKey(pluginAndTarget)
    ) {
      return
    }

    lastCheckDate[pluginAndTarget] = Instant.now()
    inProgress.add(pluginAndTarget)
    val task = VerifyPluginTask(
        verifierExecutor,
        pluginAndTarget.updateInfo,
        (pluginAndTarget.verificationTarget as VerificationTarget.Ide).ideVersion,
        jdkPath,
        pluginDetailsCache,
        ideDescriptorsCache,
        jdkDescriptorsCache
    )

    val taskStatus = taskManager.enqueue(
        task,
        { taskResult, taskStatus -> onSuccess(taskResult, taskStatus) },
        { error, _ -> onError(pluginAndTarget, error) },
        { _, _ -> },
        { onCompletion(pluginAndTarget) }
    )
    logger.info("Verification $pluginAndTarget is scheduled with task #${taskStatus.taskId}")
  }

  @Synchronized
  private fun onCompletion(pluginAndTarget: PluginAndTarget) {
    inProgress.remove(pluginAndTarget)
  }

  @Synchronized
  private fun onError(pluginAndTarget: PluginAndTarget, error: Throwable) {
    logger.error("Unable to check $pluginAndTarget", error)
  }

  private fun onSuccess(result: VerificationResult, taskStatus: ServiceTaskStatus) {
    logger.info("Verified ${result.plugin} against ${result.verificationTarget}: $result")
    val decision = verificationResultsFilter.shouldSendVerificationResult(result, taskStatus.endTime!!)
    if (decision == VerificationResultFilter.Result.Send) {
      try {
        verifierServiceProtocol.sendVerificationResult(result)
      } catch (e: Exception) {
        logger.error("Unable to send verification result for ${result.plugin}", e)
      }
    } else if (decision is VerificationResultFilter.Result.Ignore) {
      logger.info("Verification result for ${result.plugin} against ${result.verificationTarget} has been ignored: ${decision.ignoreReason}")
    }
  }

  override fun onStop() {
    verifierExecutor.close()
  }
}