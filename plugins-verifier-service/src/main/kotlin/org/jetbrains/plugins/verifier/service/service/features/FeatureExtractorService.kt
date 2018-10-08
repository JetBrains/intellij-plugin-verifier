package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.network.ServerUnavailable503Exception
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
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
                              private val pluginDetailsCache: PluginDetailsCache,
                              private val ideRepository: IdeRepository)
  : BaseService("FeatureService", 0, 5, TimeUnit.MINUTES, taskManager) {

  private val scheduledUpdates = linkedMapOf<UpdateInfo, TaskDescriptor>()

  override fun doServe() {
    val updatesToExtract = try {
      featureServiceProtocol.getUpdatesToExtract()
    } catch (e: ServerUnavailable503Exception) {
      logger.info("Marketplace ${e.serverUrl} is currently unavailable (HTTP 503)")
      return
    }

    logger.info("Extracting features of ${updatesToExtract.size} updates")
    for (updateInfo in updatesToExtract) {
      if (updateInfo !in scheduledUpdates) {
        schedule(updateInfo)
      }
    }
  }

  private fun schedule(updateInfo: UpdateInfo) {
    val extractTask = ExtractFeaturesTask(
        updateInfo,
        ideDescriptorsCache,
        pluginDetailsCache,
        ideRepository
    )
    val taskDescriptor = taskManager.enqueue(
        extractTask,
        { result, _ -> onSuccess(result) },
        { t, tid -> onError(t, tid, extractTask) },
        { _ -> onCompletion(extractTask) }
    )
    scheduledUpdates[updateInfo] = taskDescriptor
    logger.info("Schedule extraction of features for $updateInfo with taskId #${taskDescriptor.taskId}")
  }

  @Synchronized
  private fun onCompletion(task: ExtractFeaturesTask) {
    scheduledUpdates.remove(task.updateInfo)
  }

  @Synchronized
  private fun onError(
      error: Throwable,
      taskDescriptor: TaskDescriptor,
      task: ExtractFeaturesTask
  ) {
    logger.error("Unable to extract features of ${task.updateInfo} (#${taskDescriptor.taskId})", error)
  }

  @Synchronized
  private fun pauseFeaturesExtraction() {
    for ((update, taskDescriptor) in scheduledUpdates.entries) {
      logger.info("Cancel extraction of features for $update")
      taskManager.cancel(taskDescriptor)
    }
    scheduledUpdates.clear()
  }

  //Do not synchronize: results sending is performed from background threads.
  private fun onSuccess(result: ExtractFeaturesTask.Result) {
    with(result) {
      logger.info("Plugin $updateInfo is processed: $result")
      try {
        featureServiceProtocol.sendExtractedFeatures(this)
      } catch (e: ServerUnavailable503Exception) {
        logger.info("Marketplace ${e.serverUrl} is currently unavailable. Stop all the scheduled updates.")
        pauseFeaturesExtraction()
      } catch (e: Exception) {
        logger.error("Failed to send features result for $updateInfo", e)
      }
    }
  }


}