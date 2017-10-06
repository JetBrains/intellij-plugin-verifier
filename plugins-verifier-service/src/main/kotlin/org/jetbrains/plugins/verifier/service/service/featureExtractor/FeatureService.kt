package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.api.prepareFeaturesResponse
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.ServerInstance
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskStatus
import org.jetbrains.plugins.verifier.service.util.UpdateInfoCache
import org.jetbrains.plugins.verifier.service.util.createJsonRequestBody
import org.jetbrains.plugins.verifier.service.util.createStringRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class FeatureService : BaseService("FeatureService", 0, 5, TimeUnit.MINUTES) {

  private val UPDATE_PROCESS_MIN_PAUSE_MILLIS = TimeUnit.MINUTES.toMillis(10)

  private val inProgressUpdates: MutableSet<UpdateInfo> = hashSetOf()

  private val lastProceedDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  private val repo2FeatureExtractorApi = hashMapOf<String, FeaturesApi>()

  private fun getFeaturesApiConnector(): FeaturesApi {
    val repositoryUrl = Settings.FEATURE_EXTRACTOR_REPOSITORY_URL.get()
    return repo2FeatureExtractorApi.getOrPut(repositoryUrl, { createFeatureExtractor(repositoryUrl) })
  }

  private fun createFeatureExtractor(repositoryUrl: String): FeaturesApi = Retrofit.Builder()
      .baseUrl(repositoryUrl)
      .addConverterFactory(GsonConverterFactory.create(ServerInstance.GSON))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(FeaturesApi::class.java)

  override fun doServe() {
    val updatesToExtract = getUpdatesToExtract()
    LOG.info("Extracting features of ${updatesToExtract.size} updates: $updatesToExtract")
    for (update in updatesToExtract) {
      if (inProgressUpdates.size > 500) {
        return
      }
      schedule(update)
    }
  }

  private fun schedule(updateId: Int) {
    val updateInfo = UpdateInfoCache.getUpdateInfo(updateId) ?: return
    if (updateInfo in inProgressUpdates) {
      return
    }

    val lastProceedAgo = System.currentTimeMillis() - (lastProceedDate[updateInfo] ?: 0)
    if (lastProceedAgo < UPDATE_PROCESS_MIN_PAUSE_MILLIS) {
      return
    }

    lastProceedDate[updateInfo] = System.currentTimeMillis()

    val runner = ExtractFeaturesTask(PluginCoordinate.ByUpdateInfo(updateInfo, ServerInstance.pluginRepository), updateInfo)
    val taskStatus = taskManager.enqueue(
        runner,
        { onSuccess(it) },
        { t, tid, task -> onError(t, tid, task) },
        { _, task -> onCompletion(task) }
    )
    inProgressUpdates.add(updateInfo)
    LOG.info("Extract features of $updateInfo is scheduled with taskId #${taskStatus.taskId}")
  }

  private fun onCompletion(task: ExtractFeaturesTask) {
    inProgressUpdates.remove((task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo)
  }

  private fun onError(error: Throwable, taskStatus: TaskStatus, task: ExtractFeaturesTask) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    LOG.error("Unable to extract features of $updateInfo (#${taskStatus.taskId})", error)
  }

  private fun onSuccess(extractorResult: FeaturesResult) {
    val updateInfo = extractorResult.updateInfo
    val resultType = extractorResult.resultType
    val size = extractorResult.features.size
    LOG.info("Plugin $updateInfo extracted $size features: ($resultType)")

    val pluginsResult = prepareFeaturesResponse(updateInfo, resultType, extractorResult.features)
    try {
      sendExtractedFeatures(pluginsResult, pluginRepositoryUserName, pluginRepositoryPassword).executeSuccessfully()
    } catch(e: Exception) {
      LOG.error("Unable to send check result of the plugin ${extractorResult.updateInfo}", e)
    }
  }

  private fun getUpdatesToExtract(): List<Int> = getUpdatesToExtractFeatures(pluginRepositoryUserName, pluginRepositoryPassword).executeSuccessfully().body().sortedDescending()

  private fun getUpdatesToExtractFeatures(userName: String, password: String) =
      getFeaturesApiConnector().getUpdatesToExtractFeatures(createStringRequestBody(userName), createStringRequestBody(password))


  private fun sendExtractedFeatures(featuresJsonResponse: String, userName: String, password: String) =
      getFeaturesApiConnector().sendExtractedFeatures(createJsonRequestBody(featuresJsonResponse), createStringRequestBody(userName), createStringRequestBody(password))

}