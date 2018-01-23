package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.ide.IdeKeeper
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskManager
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
                      private val ideKeeper: IdeKeeper,
                      private val pluginDetailsCache: PluginDetailsCache,
                      private val ideDescriptorsCache: IdeDescriptorsCache,
                      private val jdkPath: JdkPath)

  : BaseService("VerifierService", 0, 5, TimeUnit.MINUTES, taskManager) {

  /**
   * Descriptor of the plugin and IDE against which the plugin is to be verified.
   */
  private data class PluginAndIdeVersion(val updateInfo: UpdateInfo, val ideVersion: IdeVersion) {
    override fun toString() = "$updateInfo against $ideVersion"
  }

  private val inProgress = hashSetOf<PluginAndIdeVersion>()

  private val lastCheckDate = hashMapOf<PluginAndIdeVersion, Instant>()

  private val verifierExecutor = Verification.createVerifierExecutor(pluginDetailsCache, jdkDescriptorsCache)

  override fun doServe() {
    val pluginsToCheck = requestPluginsToCheck()
    logger.info("Checking updates: $pluginsToCheck")
    for (pluginAndIdeVersion in pluginsToCheck) {
      if (inProgress.size > 500) {
        return
      }

      if (pluginAndIdeVersion !in inProgress && !isCheckedRecently(pluginAndIdeVersion)) {
        schedule(pluginAndIdeVersion)
      }
    }
  }

  private fun isCheckedRecently(pluginAndIdeVersion: PluginAndIdeVersion): Boolean {
    val lastCheckTime = lastCheckDate[pluginAndIdeVersion] ?: Instant.EPOCH
    val now = Instant.now()
    return lastCheckTime.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
  }

  private fun requestPluginsToCheck(): List<PluginAndIdeVersion> {
    return ideKeeper.getAvailableIdeVersions().flatMap { ideVersion ->
      verifierServiceProtocol.requestUpdatesToCheck(ideVersion).map {
        PluginAndIdeVersion(it, ideVersion)
      }
    }
  }

  private fun schedule(pluginAndIdeVersion: PluginAndIdeVersion) {
    lastCheckDate[pluginAndIdeVersion] = Instant.now()
    inProgress.add(pluginAndIdeVersion)
    val task = VerifyPluginTask(
        verifierExecutor,
        pluginAndIdeVersion.updateInfo,
        jdkPath,
        pluginAndIdeVersion.ideVersion,
        pluginDetailsCache,
        ideDescriptorsCache
    )

    val taskStatus = taskManager.enqueue(
        task,
        { taskResult, _ -> onSuccess(pluginAndIdeVersion, taskResult) },
        { error, _ -> onError(pluginAndIdeVersion, error) },
        { onCompletion(pluginAndIdeVersion) }
    )
    logger.info("Check $pluginAndIdeVersion is scheduled #${taskStatus.taskId}")
  }

  private fun onCompletion(pluginAndIdeVersion: PluginAndIdeVersion) {
    inProgress.remove(pluginAndIdeVersion)
  }

  private fun onError(pluginAndIdeVersion: PluginAndIdeVersion, error: Throwable) {
    logger.error("Unable to check $pluginAndIdeVersion", error)
  }

  private fun onSuccess(pluginAndIdeVersion: PluginAndIdeVersion, result: VerificationResult) {
    logger.info("Checked $pluginAndIdeVersion: $result")
    try {
      verifierServiceProtocol.sendVerificationResult(result)
    } catch (e: Exception) {
      logger.error("Unable to send verification result of checking $pluginAndIdeVersion", e)
    }
  }

  override fun onStop() {
    super.onStop()
    verifierExecutor.close()
  }
}