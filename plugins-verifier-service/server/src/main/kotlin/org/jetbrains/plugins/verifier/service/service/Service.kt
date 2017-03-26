package org.jetbrains.plugins.verifier.service.service

import com.google.common.collect.HashMultimap
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.Gson
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.configurations.CheckRangeResults
import com.jetbrains.pluginverifier.format.UpdateInfo
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.api.TaskStatus
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.runners.CheckRangeRunner
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
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

object Service {

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

  private val UPDATE_MISSING_IDE_PAUSE_MILLIS = TimeUnit.DAYS.toMillis(1)

  private val UPDATE_CHECK_MIN_PAUSE_MILLIS = TimeUnit.MINUTES.toMillis(10)

  private val verifiableUpdates: MutableMap<UpdateInfo, TaskId> = hashMapOf()
  private val lastCheckDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  val updatesMissingCompatibleIde: MutableSet<UpdateInfo> = hashSetOf()

  private var isRequesting: Boolean = false

  private val verifier: VerificationApi = Retrofit.Builder()
      .baseUrl(Settings.PLUGIN_REPOSITORY_URL.get())
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeClient(LOG.isDebugEnabled))
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

  private val userName: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()
  }

  private val password: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()
  }

  @Synchronized
  fun tick() {
    LOG.info("It's time to check more plugins!")

    if (isServerTooBusy()) return

    if (isRequesting) {
      LOG.info("The server is already requesting new plugins list")
      return
    }

    isRequesting = true

    try {

      val tasks = HashMultimap.create<Int, IdeVersion>()

      for (ideVersion in IdeFilesManager.ideList()) {
        getUpdatesToCheck(ideVersion).forEach {
          tasks.put(it, ideVersion)
        }
      }

      for ((updateId, ideVersions) in tasks.asMap()) {
        if (isServerTooBusy()) {
          return
        }
        schedule(updateId, ideVersions.toList())
      }

    } catch (e: Exception) {
      LOG.error("Failed to schedule updates check", e)
    } finally {
      isRequesting = false
    }
  }

  private fun getUpdatesToCheck(ideVersion: IdeVersion): List<Int> {
    val updatesToCheck: List<Int> = getUpdatesToCheck(ideVersion, userName, password).executeSuccessfully().body()
    LOG.info("Repository get updates to check with #$ideVersion success: (total: ${updatesToCheck.size}): $updatesToCheck")
    return updatesToCheck
  }

  private fun schedule(updateId: Int, versions: List<IdeVersion>) {
    val updateInfo = UpdateInfoCache.getUpdateInfo(updateId) ?: return
    schedule(updateInfo, versions)
  }

  private fun schedule(updateInfo: UpdateInfo, versions: List<IdeVersion>) {
    if (updateInfo in verifiableUpdates) {
      LOG.debug("Update $updateInfo is currently being verified; ignore verification of this update")
      return
    }
    val lastCheck = lastCheckDate[updateInfo]
    if (updatesMissingCompatibleIde.contains(updateInfo) && lastCheck != null && System.currentTimeMillis() - lastCheck < UPDATE_MISSING_IDE_PAUSE_MILLIS) {
      return
    }
    if (lastCheck != null && System.currentTimeMillis() - lastCheck < UPDATE_CHECK_MIN_PAUSE_MILLIS) {
      LOG.info("Update $updateInfo was checked recently; wait at least ${UPDATE_CHECK_MIN_PAUSE_MILLIS / 1000} seconds;")
      return
    }

    val runner = CheckRangeRunner(PluginDescriptor.ByUpdateInfo(updateInfo), getRunnerParams(), versions)
    val taskId = TaskManager.enqueue(
        runner,
        { onSuccess(it, updateInfo) },
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

  private fun onSuccess(result: Result<CheckRangeResults>, updateInfo: UpdateInfo) {
    val results = result.result!!
    LOG.info("Update ${results.plugin} is successfully checked with IDE-s. Result type = ${results.resultType}; " +
        "IDE-s = ${results.checkedIdeList?.joinToString()} " +
        (if (results.badPlugin != null) "bad plugin = ${results.badPlugin}; " else "") +
        "in ${result.taskStatus.elapsedTime() / 1000} s")

    if (results.resultType == CheckRangeResults.ResultType.NO_COMPATIBLE_IDES) {
      updatesMissingCompatibleIde.add(updateInfo)
    } else {
      updatesMissingCompatibleIde.remove(updateInfo)
    }

    try {
      sendUpdateCheckResult(results, userName, password).executeSuccessfully()
    } catch(e: Exception) {
      LOG.error("Unable to send check result of the plugin ${results.plugin}", e)
    }
  }

  fun getUpdatesToCheck(availableIde: IdeVersion, userName: String, password: String) =
      verifier.getUpdatesToCheck(availableIde.asString(), createStringRequestBody(userName), createStringRequestBody(password))

  private fun sendUpdateCheckResult(checkResult: CheckRangeResults, userName: String, password: String) =
      verifier.sendUpdateCheckResult(createCompactJsonRequestBody(checkResult), createStringRequestBody(userName), createStringRequestBody(password))

}

interface VerificationApi {

  @POST("/verification/getUpdatesToCheck")
  @Multipart
  fun getUpdatesToCheck(@Part("availableIde") availableIde: String,
                        @Part("userName") userName: RequestBody,
                        @Part("password") password: RequestBody): Call<List<Int>>

  @POST("/verification/receiveUpdateCheckResult")
  @Multipart
  fun sendUpdateCheckResult(@Part("checkResults") checkResult: RequestBody,
                            @Part("userName") userName: RequestBody,
                            @Part("password") password: RequestBody): Call<ResponseBody>

}