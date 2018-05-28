package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
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

  override fun doServe() {
    val updatesToExtract = featureServiceProtocol.getUpdatesToExtract()
    logger.info("Extracting features of ${updatesToExtract.size} updates")
    for (updateInfo in updatesToExtract) {
      if (updateInfo !in inProgressUpdates) {
        schedule(updateInfo)
      }
    }
  }

  private fun schedule(updateInfo: UpdateInfo) {
    val extractTask = ExtractFeaturesTask(
        updateInfo,
        ideDescriptorsCache,
        pluginDetailsCache
    )
    val taskDescriptor = taskManager.enqueue(
        extractTask,
        { result, _ -> onSuccess(result) },
        { t, tid -> onError(t, tid, extractTask) },
        { _, _ -> },
        { _ -> onCompletion(extractTask) }
    )
    inProgressUpdates.add(updateInfo)
    logger.info("Extract features of $updateInfo is scheduled with taskId #${taskDescriptor.taskId}")
  }

  @Synchronized
  private fun onCompletion(task: ExtractFeaturesTask) {
    inProgressUpdates.remove(task.updateInfo)
  }

  private fun onError(error: Throwable, taskDescriptor: TaskDescriptor, task: ExtractFeaturesTask) {
    logger.error("Unable to extract features of ${task.updateInfo} (#${taskDescriptor.taskId})", error)
  }

  private fun onSuccess(result: ExtractFeaturesTask.Result) {
    with(result) {
      logger.info("Plugin $updateInfo is processed: ${result.presentableText()}")
      try {
        featureServiceProtocol.sendExtractedFeatures(this)
      } catch (e: Exception) {
        logger.error("Failed to send features result for ${updateInfo}", e)
      }
    }
  }


}