package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Feature service is responsible for extracting the plugin features
 * using the _intellij-feature-extractor_ tool.
 *
 * This service periodically accesses the plugin repository, fetches plugins of which features should be extracted,
 * and sends the features' reports.
 *
 * See [Feature extractor integration with the plugin repository](https://confluence.jetbrains.com/display/PLREP/features-extractor+integration+with+the+plugins.jetbrains.com)
 */
class FeatureExtractorService(taskManager: TaskManager,
                              private val featureServiceProtocol: FeatureServiceProtocol,
                              private val ideDescriptorsCache: IdeDescriptorsCache,
                              private val pluginDetailsCache: PluginDetailsCache)
  : BaseService("FeatureService", 0, 5, TimeUnit.MINUTES, taskManager) {

  private val inProgressUpdates = hashSetOf<UpdateInfo>()

  private val lastProceedDate = hashMapOf<UpdateInfo, Instant>()

  override fun doServe() {
    val updatesToExtract = featureServiceProtocol.getUpdatesToExtract()
    logger.info("Extracting features of ${updatesToExtract.size} updates")
    for (updateInfo in updatesToExtract) {
      if (!isProcessedRecently(updateInfo)) {
        schedule(updateInfo)
      }
    }
  }

  private fun isProcessedRecently(updateInfo: UpdateInfo): Boolean {
    val now = Instant.now()
    val lastDate = lastProceedDate[updateInfo] ?: Instant.EPOCH
    return lastDate.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
  }

  private fun schedule(updateInfo: UpdateInfo) {
    lastProceedDate[updateInfo] = Instant.now()

    val runner = ExtractFeaturesTask(
        updateInfo,
        ideDescriptorsCache,
        pluginDetailsCache
    )
    val taskDescriptor = taskManager.enqueue(
        runner,
        { result, _ -> result.onSuccess() },
        { t, tid -> onError(t, tid, runner) },
        { _, _ -> },
        { _ -> onCompletion(runner) }
    )
    inProgressUpdates.add(updateInfo)
    logger.info("Extract features of $updateInfo is scheduled with taskId #${taskDescriptor.taskId}")
  }

  private fun onCompletion(task: ExtractFeaturesTask) {
    inProgressUpdates.remove(task.updateInfo)
  }

  private fun onError(error: Throwable, taskDescriptor: TaskDescriptor, task: ExtractFeaturesTask) {
    logger.error("Unable to extract features of ${task.updateInfo} (#${taskDescriptor.taskId})", error)
  }

  private fun ExtractFeaturesTask.Result.onSuccess() {
    logger.info("For plugin $updateInfo there " + "is".pluralize(features.size) + " " + "feature".pluralizeWithNumber(features.size) + " extracted: $resultType")
    try {
      featureServiceProtocol.sendExtractedFeatures(this)
    } catch (e: Exception) {
      logger.error("Unable to send extracted features of the plugin ${this.updateInfo}", e)
    }
  }


}