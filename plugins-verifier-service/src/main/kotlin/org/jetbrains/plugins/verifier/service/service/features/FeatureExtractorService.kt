package org.jetbrains.plugins.verifier.service.service.features

import com.google.gson.Gson
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.createJsonRequestBody
import com.jetbrains.pluginverifier.network.createStringRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
                              private val repositoryUrl: String) : BaseService("FeatureService", 0, 5, TimeUnit.MINUTES, serverContext) {

  private val inProgressUpdates: MutableSet<UpdateInfo> = hashSetOf()

  private val lastProceedDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  private val repo2FeatureExtractorApi = hashMapOf<String, FeaturesPluginRepositoryConnector>()

  private fun getFeaturesApiConnector(): FeaturesPluginRepositoryConnector =
      repo2FeatureExtractorApi.getOrPut(repositoryUrl, { createFeatureExtractor(repositoryUrl) })

  private fun createFeatureExtractor(repositoryUrl: String): FeaturesPluginRepositoryConnector = Retrofit.Builder()
      .baseUrl(repositoryUrl)
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(FeaturesPluginRepositoryConnector::class.java)

  override fun doServe() {
    val updatesToExtract = getUpdatesToExtract()
    logger.info("Extracting features of ${updatesToExtract.size} updates: $updatesToExtract")
    for (update in updatesToExtract) {
      if (inProgressUpdates.size > 500) {
        return
      }
      schedule(update)
    }
  }

  private fun schedule(updateId: Int) {
    val updateInfo = serverContext.pluginRepository.getPluginInfoById(updateId) ?: return
    if (updateInfo in inProgressUpdates) {
      return
    }

    val lastProceedAgo = System.currentTimeMillis() - (lastProceedDate[updateInfo] ?: 0)
    if (lastProceedAgo < UPDATE_PROCESS_MIN_PAUSE_MILLIS) {
      return
    }

    lastProceedDate[updateInfo] = System.currentTimeMillis()

    val runner = ExtractFeaturesTask(
        serverContext,
        PluginCoordinate.ByUpdateInfo(updateInfo, serverContext.pluginRepository),
        updateInfo
    )
    val taskStatus = serverContext.taskManager.enqueue(
        runner,
        { result, _ -> onSuccess(result) },
        { t, tid -> onError(t, tid, runner) },
        { _ -> onCompletion(runner) }
    )
    inProgressUpdates.add(updateInfo)
    logger.info("Extract features of $updateInfo is scheduled with taskId #${taskStatus.taskId}")
  }

  private fun onCompletion(task: ExtractFeaturesTask) {
    inProgressUpdates.remove((task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo)
  }

  private fun onError(error: Throwable, taskStatus: ServiceTaskStatus, task: ExtractFeaturesTask) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    logger.error("Unable to extract features of $updateInfo (#${taskStatus.taskId})", error)
  }

  private fun onSuccess(extractorResult: ExtractFeaturesTask.Result) {
    val updateInfo = extractorResult.updateInfo
    val resultType = extractorResult.resultType
    val size = extractorResult.features.size
    logger.info("Plugin $updateInfo extracted $size features: ($resultType)")

    val pluginsResult = prepareFeaturesResponse(updateInfo, resultType, extractorResult.features)
    try {
      sendExtractedFeatures(pluginsResult).executeSuccessfully()
    } catch (e: Exception) {
      logger.error("Unable to send check result of the plugin ${extractorResult.updateInfo}", e)
    }
  }

  private fun getUpdatesToExtract(): List<Int> = getUpdatesToExtractFeatures().executeSuccessfully().body().sortedDescending()

  private val userNameRequestBody = createStringRequestBody(serverContext.authorizationData.pluginRepositoryUserName)

  private val passwordRequestBody = createStringRequestBody(serverContext.authorizationData.pluginRepositoryPassword)

  private fun getUpdatesToExtractFeatures() =
      getFeaturesApiConnector().getUpdatesToExtractFeatures(userNameRequestBody, passwordRequestBody)


  private fun sendExtractedFeatures(featuresJsonResponse: String) =
      getFeaturesApiConnector().sendExtractedFeatures(createJsonRequestBody(featuresJsonResponse), userNameRequestBody, passwordRequestBody)

  companion object {
    private val UPDATE_PROCESS_MIN_PAUSE_MILLIS = TimeUnit.MINUTES.toMillis(10)
  }

}