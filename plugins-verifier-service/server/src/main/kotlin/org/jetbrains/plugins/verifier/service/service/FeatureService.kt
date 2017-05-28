package org.jetbrains.plugins.verifier.service.service

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.repository.UpdateInfo
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.api.TaskStatus
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.runners.ExtractFeaturesRunner
import org.jetbrains.plugins.verifier.service.runners.FeaturesResult
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.util.executeSuccessfully
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
object FeatureService {

  private val LOG: Logger = LoggerFactory.getLogger(FeatureService::class.java)

  private val featuresExtractor: FeaturesApi = Retrofit.Builder()
      .baseUrl(Settings.FEATURE_EXTRACTOR_REPOSITORY_URL.get())
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeClient(LOG.isDebugEnabled))
      .build()
      .create(FeaturesApi::class.java)

  fun run() {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("feature-service-%d")
            .build()
    ).scheduleAtFixedRate({ tick() }, 0, SERVICE_PERIOD, TimeUnit.MINUTES)
  }

  //5 minutes
  private const val SERVICE_PERIOD: Long = 5

  private val inProgressUpdates: MutableMap<UpdateInfo, TaskId> = hashMapOf()

  private val lastCheckDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  //10 minutes
  private const val UPDATE_PROCESS_MIN_PAUSE_MILLIS = 10 * 60 * 1000

  private val userName: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()
  }

  private val password: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()
  }

  private var isRequesting: Boolean = false

  private fun isServerTooBusy(): Boolean {
    val runningNumber = TaskManager.runningTasksNumber()
    if (runningNumber >= TaskManager.MAX_RUNNING_TASKS) {
      LOG.info("There are too many running tasks $runningNumber >= ${TaskManager.MAX_RUNNING_TASKS}")
      return true
    }
    return false
  }

  @Synchronized
  fun tick() {
    LOG.info("It's time to extract more plugins!")

    if (isServerTooBusy()) return

    if (isRequesting) {
      LOG.info("The server is already requesting new plugins list")
      return
    }

    isRequesting = true

    try {
      for (it in getUpdatesToExtract()) {
        if (isServerTooBusy()) {
          return
        }
        schedule(it)
      }

    } catch (e: Exception) {
      LOG.error("Failed to schedule updates check", e)
    } finally {
      isRequesting = false
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
    val taskId = TaskManager.enqueue(
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

  private fun onSuccess(result: Result<FeaturesResult>) {
    val extractorResult = result.result!!
    logSuccess(extractorResult, result)
    val pluginsResult = convertToPluginsSiteResult(extractorResult)
    try {
      sendExtractedFeatures(pluginsResult, userName, password).executeSuccessfully()
    } catch(e: Exception) {
      LOG.error("Unable to send check result of the plugin ${extractorResult.plugin}", e)
    }
  }

  private fun convertToPluginsSiteResult(featuresResult: FeaturesResult): PluginsSiteResult {
    val updateId = featuresResult.plugin.updateInfo!!.updateId
    val resultType = featuresResult.resultType
    if (resultType == FeaturesResult.ResultType.BAD_PLUGIN) {
      return PluginsSiteResult(updateId, resultType)
    }
    return PluginsSiteResult(updateId, resultType, featuresResult.features)
  }

  private fun logSuccess(featuresResult: FeaturesResult, result: Result<FeaturesResult>) {
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


  private fun sendExtractedFeatures(extractedFeatures: PluginsSiteResult, userName: String, password: String) =
      featuresExtractor.sendExtractedFeatures(createCompactJsonRequestBody(extractedFeatures), createStringRequestBody(userName), createStringRequestBody(password))

}

data class PluginsSiteResult(@SerializedName("updateId") val updateId: Int,
                             @SerializedName("resultType") val resultType: FeaturesResult.ResultType,
                             @SerializedName("features") val features: List<ExtensionPointFeatures> = emptyList())

interface FeaturesApi {

  @Multipart
  @POST("/feature/getUpdatesToExtractFeatures")
  fun getUpdatesToExtractFeatures(@Part("userName") userName: RequestBody,
                                  @Part("password") password: RequestBody): Call<List<Int>>

  @Multipart
  @POST("/feature/receiveExtractedFeatures")
  fun sendExtractedFeatures(@Part("extractedFeatures") extractedFeatures: RequestBody,
                            @Part("userName") userName: RequestBody,
                            @Part("password") password: RequestBody): Call<ResponseBody>

}
