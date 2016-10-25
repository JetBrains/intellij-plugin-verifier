package org.jetbrains.plugins.verifier.service.service

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.configurations.CheckRangeResults
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.persistence.GsonHolder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.api.TaskStatus
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.runners.CheckRangeRunner
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.util.MultipartUtil
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

private object Service {

  fun run() {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("service-%d")
            .build()
    ).scheduleAtFixedRate({ Service.tick() }, 0, SERVICE_PERIOD, TimeUnit.MINUTES)
  }

  private val LOG: Logger = LoggerFactory.getLogger(Service::class.java)

  //5 minutes
  private const val SERVICE_PERIOD: Long = 5

  //10 minutes
  private const val UPDATE_CHECK_MIN_PAUSE_MILLIS = 10 * 60 * 1000

  private val verifiableUpdates: MutableMap<UpdateInfo, TaskId> = hashMapOf()
  private val lastCheckDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  private var isRequesting: Boolean = false

  private fun makeClient(): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.MINUTES)
      .readTimeout(5, TimeUnit.MINUTES)
      .writeTimeout(5, TimeUnit.MINUTES)
      .addInterceptor(HttpLoggingInterceptor().setLevel(if (LOG.isDebugEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
      .build()

  private val verifier: VerificationApi = Retrofit.Builder()
      .baseUrl(Settings.PLUGIN_REPOSITORY_URL.get())
      .addConverterFactory(GsonConverterFactory.create(GsonHolder.GSON))
      .client(makeClient())
      .build()
      .create(VerificationApi::class.java)

  private fun isServerTooBusy(): Boolean {
    val runningNumber = TaskManager.runningTasksNumber()
    if (runningNumber >= TaskManager.MAX_RUNNING_TASKS) {
      LOG.info("There are too many running tasks $runningNumber >= ${TaskManager.MAX_RUNNING_TASKS}")
      return true
    }
    return false
  }

  private val userName: MultipartBody.Part by lazy {
    val userName = Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()
    MultipartUtil.createJsonPart("username", userName)
  }

  private val password: MultipartBody.Part by lazy {
    val password = Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()
    MultipartUtil.createJsonPart("password", password)
  }

  @Synchronized
  fun tick() {
    LOG.info("It's time to check more plugins!")

    if (isRequesting) {
      LOG.info("The server is already requesting new plugins list")
      return
    }
    isRequesting = true

    if (isServerTooBusy()) return

    try {
      val ideList = IdeFilesManager.ideList()
      val availableIdeList = MultipartUtil.createJsonPart("availableIdeList", ideList)
      val updatesToCheck = verifier.getUpdatesToCheck(availableIdeList, userName, password).executeSuccessfully().body()
      LOG.info("Repository connection success. Updates to check (${updatesToCheck.size} of them): $updatesToCheck")

      for ((updateInfo, ideVersions) in updatesToCheck.filter { it.ideVersions.isNotEmpty() }) {
        if (isServerTooBusy()) {
          break
        }
        schedule(updateInfo, ideVersions)
      }

    } catch (e: Exception) {
      LOG.error("Failed to schedule updates check", e)
    } finally {
      isRequesting = false
    }
  }

  private fun schedule(updateInfo: UpdateInfo, versions: List<IdeVersion>) {
    if (updateInfo in verifiableUpdates) {
      LOG.info("Update $updateInfo is currently being verified; ignore verification of this update")
      return
    }
    val lastCheck = lastCheckDate[updateInfo]
    if (lastCheck != null && System.currentTimeMillis() - lastCheck < UPDATE_CHECK_MIN_PAUSE_MILLIS) {
      LOG.info("Update $updateInfo was checked recently; wait at least ${UPDATE_CHECK_MIN_PAUSE_MILLIS / 1000} seconds;")
      return
    }

    val runner = CheckRangeRunner(PluginDescriptor.ByUpdateInfo(updateInfo), getRunnerParams(), versions)
    val taskId = TaskManager.enqueue(
        runner,
        { onSuccess(it) },
        { t, tid, task -> logError(t, tid, task as CheckRangeRunner) },
        { tst, task -> onUpdateChecked(task as CheckRangeRunner) }
    )
    verifiableUpdates[updateInfo] = taskId
    lastCheckDate[updateInfo] = System.currentTimeMillis()
    LOG.info("Check range for $updateInfo is scheduled with taskId #$taskId")
  }

  private fun getRunnerParams(): CheckRangeRunnerParams = CheckRangeRunnerParams(JdkVersion.JAVA_8_ORACLE, VOptions())

  @Synchronized
  private fun releaseUpdate(updateInfo: UpdateInfo) {
    LOG.info("Update $updateInfo is checked and is ready to be checked again")
    verifiableUpdates.remove(updateInfo)
  }

  private fun onUpdateChecked(task: CheckRangeRunner) {
    val updateInfo = (task.pluginToCheck as PluginDescriptor.ByUpdateInfo).updateInfo
    releaseUpdate(updateInfo)
  }

  private fun logError(throwable: Throwable, taskStatus: TaskStatus, task: CheckRangeRunner) {
    val updateInfo = (task.pluginToCheck as PluginDescriptor.ByUpdateInfo).updateInfo
    LOG.error("Unable to check update $updateInfo: taskId = #${taskStatus.taskId}", throwable)
  }

  private fun onSuccess(result: Result<CheckRangeResults>) {
    val results = result.result!!
    LOG.info("Update ${results.plugin} is successfully checked with IDE-s. Result type = ${results.resultType}; " +
        "IDE-s = ${results.checkedIdeList?.joinToString()} " +
        "bad plugin = ${results.badPlugin}; " +
        "in ${result.taskStatus.elapsedTime() / 1000} s")

    try {
      verifier.sendUpdateCheckResult(results, userName, password).executeSuccessfully()
    } catch(e: Exception) {
      LOG.error("Unable to send check result of the plugin ${results.plugin}", e)
    }
  }

}

data class UpdateToCheck(@SerializedName("updateInfo") val updateInfo: UpdateInfo,
                         @SerializedName("ideVersions") val ideVersions: List<IdeVersion>)

interface VerificationApi {

  @Multipart
  @POST("/verification/getUpdatesToCheck")
  fun getUpdatesToCheck(@Part availableIdeList: MultipartBody.Part,
                        @Part userName: MultipartBody.Part,
                        @Part password: MultipartBody.Part): Call<List<UpdateToCheck>>

  @Multipart
  @POST("/verification/receiveUpdateCheckResult")
  fun sendUpdateCheckResult(@Part("checkResults") checkResult: CheckRangeResults,
                            @Part userName: MultipartBody.Part,
                            @Part password: MultipartBody.Part): Call<ResponseBody>

}