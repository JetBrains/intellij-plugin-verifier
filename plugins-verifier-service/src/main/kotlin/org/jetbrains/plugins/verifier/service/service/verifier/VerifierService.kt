package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.common.collect.HashMultimap
import com.google.gson.Gson
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.misc.executeSuccessfully
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.params.JdkVersion
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
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit

class VerifierService(taskManager: TaskManager) : BaseService("VerifierService", 0, 5, TimeUnit.MINUTES, taskManager) {

  private val UPDATE_MISSING_IDE_PAUSE_MILLIS = TimeUnit.DAYS.toMillis(1)

  private val UPDATE_CHECK_MIN_PAUSE_MILLIS = TimeUnit.MINUTES.toMillis(10)

  private val verifiableUpdates: MutableMap<UpdateInfo, TaskId> = hashMapOf()

  private val lastCheckDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  private val verifier: VerificationApi = Retrofit.Builder()
      .baseUrl(Settings.PLUGIN_REPOSITORY_URL.get())
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(LOG.isDebugEnabled, 5, TimeUnit.MINUTES))
      .build()
      .create(VerificationApi::class.java)

  private val userName: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()
  }

  private val password: String by lazy {
    Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()
  }

  @Synchronized
  override fun doTick() {
    LOG.info("It's time to check the plugins")

    val tasks = HashMultimap.create<Int, IdeVersion>()

    for (ideVersion in IdeFilesManager.ideList()) {
      getUpdatesToCheck(ideVersion).forEach {
        tasks.put(it, ideVersion)
      }
    }

    for ((updateId, ideVersions) in tasks.asMap()) {
      if (taskManager.isBusy()) {
        return
      }
      schedule(updateId, ideVersions.toList())
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

    val pluginInfo: PluginInfo = PluginInfo(updateInfo.pluginId, updateInfo.version, updateInfo)
    val pluginCoordinate = PluginCoordinate.ByUpdateInfo(updateInfo)
    val runner = CheckPluginSinceUntilRangeTask(pluginInfo, pluginCoordinate, getRunnerParams(), versions)
    val taskId = taskManager.enqueue(
        runner,
        { onSuccess(it, updateInfo) },
        { t, tid, task -> logError(t, tid, task as CheckPluginSinceUntilRangeTask) },
        { _, task -> onUpdateChecked(task as CheckPluginSinceUntilRangeTask) }
    )
    verifiableUpdates[updateInfo] = taskId
    lastCheckDate[updateInfo] = System.currentTimeMillis()
    LOG.info("Check range for $updateInfo is scheduled with taskId #$taskId")
  }

  private fun getRunnerParams(): CheckRangeRunnerParams = CheckRangeRunnerParams(JdkVersion.JAVA_8_ORACLE)

  @Synchronized
  private fun releaseUpdate(updateInfo: UpdateInfo) {
    LOG.info("Update $updateInfo is checked and is ready to be checked again")
    verifiableUpdates.remove(updateInfo)
  }

  private fun onUpdateChecked(task: CheckPluginSinceUntilRangeTask) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    releaseUpdate(updateInfo)
  }

  private fun logError(throwable: Throwable, taskStatus: TaskStatus, task: CheckPluginSinceUntilRangeTask) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    LOG.error("Unable to check update $updateInfo: taskId = #${taskStatus.taskId}", throwable)
  }

  val updatesMissingCompatibleIde = ConcurrentSkipListSet<UpdateInfo>()

  private fun onSuccess(result: TaskResult<CheckRangeResults>, updateInfo: UpdateInfo) {
    val results = result.result!!
    LOG.info("Update ${results.plugin} is successfully checked with IDE-s. Result type = ${results.resultType}; " +
        "IDE-s = ${results.checkedIdeList.joinToString()} " +
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

  private fun getUpdatesToCheck(availableIde: IdeVersion, userName: String, password: String) =
      verifier.getUpdatesToCheck(availableIde.asString(), createStringRequestBody(userName), createStringRequestBody(password))

  private fun sendUpdateCheckResult(checkResult: CheckRangeResults, userName: String, password: String) =
      verifier.sendUpdateCheckResult(createCompactJsonRequestBody(checkResult), createStringRequestBody(userName), createStringRequestBody(password))

}