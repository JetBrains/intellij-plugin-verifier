package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskStatus
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
class FeatureExtractorService(serverContext: ServerContext,
                              private val featureServiceProtocol: FeatureServiceProtocol)
  : BaseService("FeatureService", 0, 5, TimeUnit.MINUTES, serverContext) {

  private val processingUpdates = hashSetOf<UpdateInfo>()

  private val lastProceedDate = hashMapOf<UpdateInfo, Instant>()

  override fun doServe() {
    val updatesToExtract = featureServiceProtocol.getUpdatesToExtract()
    logger.info("Extracting features of ${updatesToExtract.size} updates: $updatesToExtract")
    for (updateInfo in updatesToExtract) {
      if (processingUpdates.size > 500) {
        return
      }
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
        serverContext,
        updateInfo
    )
    val taskStatus = serverContext.taskManager.enqueue(
        runner,
        { result, _ -> onSuccess(result) },
        { t, tid -> onError(t, tid, runner) },
        { _ -> onCompletion(runner) }
    )
    processingUpdates.add(updateInfo)
    logger.info("Extract features of $updateInfo is scheduled with taskId #${taskStatus.taskId}")
  }

  private fun onCompletion(task: ExtractFeaturesTask) {
    processingUpdates.remove(task.updateInfo)
  }

  private fun onError(error: Throwable, taskStatus: ServiceTaskStatus, task: ExtractFeaturesTask) {
    logger.error("Unable to extract features of ${task.updateInfo} (#${taskStatus.taskId})", error)
  }

  private fun onSuccess(extractorResult: ExtractFeaturesTask.Result) {
    val updateInfo = extractorResult.updateInfo
    val resultType = extractorResult.resultType
    val size = extractorResult.features.size
    logger.info("Plugin $updateInfo extracted $size features: ($resultType)")
    try {
      featureServiceProtocol.sendExtractedFeatures(extractorResult)
    } catch (e: Exception) {
      logger.error("Unable to send extracted features of the plugin ${extractorResult.updateInfo}", e)
    }
  }


}