package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.google.gson.Gson
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.misc.executeSuccessfully
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.persistence.CompactJson
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskId
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.jetbrains.plugins.verifier.service.tasks.TaskResult
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
class FeatureService(taskManager: TaskManager) : BaseService("FeatureService", 0, 5, TimeUnit.MINUTES, taskManager) {

  private val featuresExtractor: FeaturesApi = Retrofit.Builder()
      .baseUrl(Settings.FEATURE_EXTRACTOR_REPOSITORY_URL.get())
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(FeaturesApi::class.java)

  private val inProgressUpdates: MutableMap<UpdateInfo, TaskId> = hashMapOf()

  private val lastProceedDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  private val UPDATE_PROCESS_MIN_PAUSE_MILLIS = TimeUnit.MINUTES.toMillis(10)

  override fun doTick() {
    val updatesToExtract = getUpdatesToExtract()
    LOG.info("Extracting features of ${updatesToExtract.size} updates: $updatesToExtract")
    for (update in updatesToExtract) {
      if (taskManager.isBusy()) {
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

    val pluginInfo = PluginInfo(updateInfo.pluginId, updateInfo.version, updateInfo)
    val runner = ExtractFeaturesTask(PluginCoordinate.ByUpdateInfo(updateInfo), pluginInfo)
    val taskId = taskManager.enqueue(
        runner,
        { onSuccess(it) },
        { t, tid, task -> onError(t, tid, task) },
        { _, task -> onCompletion(task) }
    )
    inProgressUpdates[updateInfo] = taskId
    LOG.info("Extract features of $updateInfo is scheduled with taskId #$taskId")
  }

  private fun onCompletion(task: ExtractFeaturesTask) {
    inProgressUpdates.remove((task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo)
  }

  private fun onError(error: Throwable, taskStatus: TaskStatus, task: ExtractFeaturesTask) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    LOG.error("Unable to extract features of $updateInfo (#${taskStatus.taskId})", error)
  }

  private fun onSuccess(result: TaskResult<FeaturesResult>) {
    val extractorResult = result.result!!
    val pluginInfo = extractorResult.plugin
    val resultType = extractorResult.resultType
    val size = extractorResult.features.size
    LOG.info("Plugin $pluginInfo extracted $size features: ($resultType)")

    val pluginsResult = convertToPluginsSiteResult(pluginInfo, resultType, extractorResult.features)
    try {
      sendExtractedFeatures(pluginsResult, pluginRepositoryUserName, pluginRepositoryPassword).executeSuccessfully()
    } catch(e: Exception) {
      LOG.error("Unable to send check result of the plugin ${extractorResult.plugin}", e)
    }
  }

  private fun convertToPluginsSiteResult(pluginInfo: PluginInfo,
                                         resultType: FeaturesResult.ResultType,
                                         features: List<ExtensionPointFeatures>): AdaptedFeaturesResult {
    val protocolVersion = Settings.PROTOCOL_VERSION.getAsInt()
    val updateId = pluginInfo.updateInfo!!.updateId
    return AdaptedFeaturesResult(updateId, resultType, features, protocolVersion)
  }


  private fun getUpdatesToExtract(): List<Int> =
      getUpdatesToExtractFeatures(pluginRepositoryUserName, pluginRepositoryPassword).executeSuccessfully().body().sortedDescending()

  private fun getUpdatesToExtractFeatures(userName: String, password: String) =
      featuresExtractor.getUpdatesToExtractFeatures(createStringRequestBody(userName), createStringRequestBody(password))


  private fun sendExtractedFeatures(extractedFeatures: AdaptedFeaturesResult, userName: String, password: String) =
      featuresExtractor.sendExtractedFeatures(createJsonRequestBody(CompactJson.toJson(extractedFeatures)), createStringRequestBody(userName), createStringRequestBody(password))

}