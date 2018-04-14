package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.service.BaseService
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
class VerifierService(taskManager: ServiceTaskManager,
                      jdkDescriptorsCache: JdkDescriptorsCache,
                      private val verifierServiceProtocol: VerifierServiceProtocol,
                      private val ideFilesBank: IdeFilesBank,
                      private val pluginDetailsCache: PluginDetailsCache,
                      private val ideDescriptorsCache: IdeDescriptorsCache,
                      private val jdkPath: JdkPath,
                      private val verificationResultsFilter: VerificationResultFilter)

  : BaseService("VerifierService", 0, 1, TimeUnit.MINUTES, taskManager) {

  companion object {
    private const val MAXIMUM_SIMULTANEOUS_VERIFICATIONS = 128
  }

  private val inProgress = hashSetOf<PluginAndIdeVersion>()

  private val lastCheckDate = hashMapOf<PluginAndIdeVersion, Instant>()

  private val verifierExecutor = Verification.createVerifierExecutor(pluginDetailsCache, jdkDescriptorsCache)

  override fun doServe() {
    val pluginsToCheck = requestPluginsToCheck()
    logger.info("There are ${pluginsToCheck.size} new plugins to be verified")
    for (pluginAndIdeVersion in pluginsToCheck) {
      if (inProgress.size > MAXIMUM_SIMULTANEOUS_VERIFICATIONS) {
        return
      }

      scheduleVerification(pluginAndIdeVersion)
    }
  }

  private fun shouldVerify(pluginAndIdeVersion: PluginAndIdeVersion) =
      pluginAndIdeVersion !in inProgress
          && !isCheckedRecently(pluginAndIdeVersion)
          && !verificationResultsFilter.ignoredVerifications.containsKey(pluginAndIdeVersion)

  private fun isCheckedRecently(pluginAndIdeVersion: PluginAndIdeVersion): Boolean {
    val lastCheckTime = lastCheckDate[pluginAndIdeVersion] ?: Instant.EPOCH
    val now = Instant.now()
    return lastCheckTime.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
  }

  private fun requestPluginsToCheck() =
      ideFilesBank.getAvailableIdeVersions()
          .flatMap { ideVersion ->
            verifierServiceProtocol
                .requestUpdatesToCheck(ideVersion)
                .map { PluginAndIdeVersion(it, ideVersion) }
          }.filter { shouldVerify(it) }

  private fun scheduleVerification(pluginAndIdeVersion: PluginAndIdeVersion) {
    lastCheckDate[pluginAndIdeVersion] = Instant.now()
    inProgress.add(pluginAndIdeVersion)
    val task = VerifyPluginTask(
        verifierExecutor,
        pluginAndIdeVersion.updateInfo,
        pluginAndIdeVersion.ideVersion,
        jdkPath,
        pluginDetailsCache,
        ideDescriptorsCache
    )

    val taskStatus = taskManager.enqueue(
        task,
        { taskResult, taskStatus -> onSuccess(taskResult, taskStatus) },
        { error, _ -> onError(pluginAndIdeVersion, error) },
        { _, _ -> },
        { onCompletion(pluginAndIdeVersion) }
    )
    logger.info("Verification $pluginAndIdeVersion is scheduled in task #${taskStatus.taskId}")
  }

  @Synchronized
  private fun onCompletion(pluginAndIdeVersion: PluginAndIdeVersion) {
    inProgress.remove(pluginAndIdeVersion)
  }

  @Synchronized
  private fun onError(pluginAndIdeVersion: PluginAndIdeVersion, error: Throwable) {
    logger.error("Unable to check $pluginAndIdeVersion", error)
  }

  private fun onSuccess(result: VerificationResult, taskStatus: ServiceTaskStatus) {
    logger.info("Verified ${result.plugin} against ${result.ideVersion}: $result")
    val decision = verificationResultsFilter.shouldSendVerificationResult(result, taskStatus.endTime!!)
    if (decision == VerificationResultFilter.Result.Send) {
      try {
        verifierServiceProtocol.sendVerificationResult(result)
      } catch (e: Exception) {
        logger.error("Unable to send verification result for ${result.plugin}", e)
      }
    } else if (decision is VerificationResultFilter.Result.Ignore) {
      logger.info("Verification result for ${result.plugin} against ${result.ideVersion} has been ignored: ${decision.ignoreReason}")
    }
  }

  override fun onStop() {
    verifierExecutor.close()
  }
}