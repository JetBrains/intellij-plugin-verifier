package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.google.gson.Gson
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.misc.executeSuccessfully
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskId
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.jetbrains.plugins.verifier.service.tasks.TaskResult
import org.jetbrains.plugins.verifier.service.tasks.TaskStatus
import org.jetbrains.plugins.verifier.service.util.UpdateInfoCache
import org.jetbrains.plugins.verifier.service.util.createCompactJsonRequestBody
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
      .client(makeOkHttpClient(LOG.isDebugEnabled, 5, TimeUnit.MINUTES))
      .build()
      .create(FeaturesApi::class.java)

  private val inProgressUpdates: MutableMap<UpdateInfo, TaskId> = hashMapOf()

  private val lastCheckDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  //10 minutes
  private val UPDATE_PROCESS_MIN_PAUSE_MILLIS = 10 * 60 * 1000

  private val userName: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()
  }

  private val password: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()
  }

  @Synchronized
  override fun doTick() {
    LOG.info("It's time to extract more plugins!")

    for (it in getUpdatesToExtract()) {
      if (taskManager.isBusy()) {
        return
      }
      schedule(it)
    }
  }

  private fun schedule(updateId: Int) {
    val info = UpdateInfoCache.getUpdateInfo(updateId) ?: return
    schedule(info)
  }

  private fun schedule(updateInfo: UpdateInfo) {
    if (updateInfo in inProgressUpdates) {
      LOG.debug("Update $updateInfo is currently in progress; ignore it")
      return
    }

    val lastCheck = lastCheckDate[updateInfo]
    if (lastCheck != null && System.currentTimeMillis() - lastCheck < UPDATE_PROCESS_MIN_PAUSE_MILLIS) {
      LOG.info("Update $updateInfo was checked recently; wait at least ${UPDATE_PROCESS_MIN_PAUSE_MILLIS / 1000} seconds;")
      return
    }

    val pluginInfo = PluginInfo(updateInfo.pluginId, updateInfo.version, updateInfo)
    val runner = ExtractFeaturesRunner(PluginCoordinate.ByUpdateInfo(updateInfo), pluginInfo)
    val taskId = taskManager.enqueue(
        runner,
        { onSuccess(it) },
        { t, tid, task -> logError(t, tid, task as ExtractFeaturesRunner) },
        { _, task -> onUpdateExtracted(task as ExtractFeaturesRunner) }
    )
    inProgressUpdates[updateInfo] = taskId
    lastCheckDate[updateInfo] = System.currentTimeMillis()
    LOG.info("Extract features of $updateInfo is scheduled with taskId #$taskId")
  }

  private fun onUpdateExtracted(task: ExtractFeaturesRunner) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    releaseUpdate(updateInfo)
  }

  @Synchronized
  private fun releaseUpdate(updateInfo: UpdateInfo) {
    LOG.info("Update $updateInfo is successfully extracted")
    inProgressUpdates.remove(updateInfo)
  }


  private fun logError(throwable: Throwable, taskStatus: TaskStatus, task: ExtractFeaturesRunner) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    LOG.error("Unable to extract features of the update $updateInfo: taskId = #${taskStatus.taskId}", throwable)
  }

  private fun onSuccess(result: TaskResult<FeaturesResult>) {
    val extractorResult = result.result!!
    logSuccess(extractorResult, result)
    val pluginsResult = convertToPluginsSiteResult(extractorResult)
    try {
      sendExtractedFeatures(pluginsResult, userName, password).executeSuccessfully()
    } catch(e: Exception) {
      LOG.error("Unable to send check result of the plugin ${extractorResult.plugin}", e)
    }
  }

  private fun convertToPluginsSiteResult(featuresResult: FeaturesResult): AdaptedFeaturesResult {
    val updateId = featuresResult.plugin.updateInfo!!.updateId
    val resultType = featuresResult.resultType
    if (resultType == FeaturesResult.ResultType.BAD_PLUGIN) {
      return AdaptedFeaturesResult(updateId, resultType)
    }
    return AdaptedFeaturesResult(updateId, resultType, featuresResult.features)
  }

  private fun logSuccess(featuresResult: FeaturesResult, result: TaskResult<FeaturesResult>) {
    val plugin = featuresResult.plugin
    val resultType = featuresResult.resultType
    val size = featuresResult.features.size
    val seconds = result.taskStatus.elapsedTime() / 1000
    LOG.info("Plugin $plugin is successfully processed; Result type = $resultType; extracted = $size features; in $seconds s")
  }


  private fun getUpdatesToExtract(): List<Int> {
    val updateIds = getUpdatesToExtractFeatures(userName, password).executeSuccessfully().body()
    LOG.info("Repository get updates to extract features success: (total: ${updateIds.size}): $updateIds")
    return updateIds
  }

  private fun getUpdatesToExtractFeatures(userName: String, password: String) =
      featuresExtractor.getUpdatesToExtractFeatures(createStringRequestBody(userName), createStringRequestBody(password))


  private fun sendExtractedFeatures(extractedFeatures: AdaptedFeaturesResult, userName: String, password: String) =
      featuresExtractor.sendExtractedFeatures(createCompactJsonRequestBody(extractedFeatures), createStringRequestBody(userName), createStringRequestBody(password))

}