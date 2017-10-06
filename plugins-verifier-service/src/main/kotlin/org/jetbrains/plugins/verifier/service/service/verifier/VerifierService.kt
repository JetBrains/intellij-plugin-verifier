package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.common.collect.LinkedHashMultimap
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.UpdateInfo
import okhttp3.ResponseBody
import org.jetbrains.plugins.verifier.service.api.prepareVerificationResponse
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.params.JdkVersion
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.ServerInstance
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskStatus
import org.jetbrains.plugins.verifier.service.util.UpdateInfoCache
import org.jetbrains.plugins.verifier.service.util.createJsonRequestBody
import org.jetbrains.plugins.verifier.service.util.createStringRequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit

class VerifierService : BaseService("VerifierService", 0, 5, TimeUnit.MINUTES) {

  private val UPDATE_MISSING_IDE_PAUSE_MS = TimeUnit.DAYS.toMillis(1)

  private val UPDATE_CHECK_MIN_PAUSE_MS = TimeUnit.MINUTES.toMillis(10)

  private val verifiableUpdates: MutableSet<UpdateInfo> = hashSetOf()

  private val lastCheckDate: MutableMap<UpdateInfo, Long> = hashMapOf()

  private val updatesMissingCompatibleIde = ConcurrentSkipListSet<UpdateInfo>(Comparator { u1, u2 -> u1.updateId - u2.updateId })

  private val repo2VerifierApi = hashMapOf<String, VerificationApi>()

  private fun getVerifierConnector(): VerificationApi {
    val repositoryUrl = Settings.VERIFIER_SERVICE_REPOSITORY_URL.get()
    return repo2VerifierApi.getOrPut(repositoryUrl, { createVerifier(repositoryUrl) })
  }

  private fun createVerifier(repositoryUrl: String): VerificationApi = Retrofit.Builder()
      .baseUrl(repositoryUrl)
      .addConverterFactory(GsonConverterFactory.create(ServerInstance.GSON))
      .client(makeOkHttpClient(LOG.isDebugEnabled, 5, TimeUnit.MINUTES))
      .build()
      .create(VerificationApi::class.java)

  override fun doServe() {
    val updateId2IdeVersions = LinkedHashMultimap.create<Int, IdeVersion>()

    for (ideVersion in IdeFilesManager.ideList()) {
      getUpdatesToCheck(ideVersion).forEach { updateId ->
        updateId2IdeVersions.put(updateId, ideVersion)
      }
    }

    LOG.info("Checking updates: ${updateId2IdeVersions.asMap()}")

    for ((updateId, ideVersions) in updateId2IdeVersions.asMap()) {
      if (verifiableUpdates.size > 500) {
        return
      }
      schedule(updateId, ideVersions.toList())
    }
  }

  private fun getUpdatesToCheck(ideVersion: IdeVersion): List<Int> =
      getUpdatesToCheck(ideVersion, pluginRepositoryUserName, pluginRepositoryPassword).executeSuccessfully().body().sortedDescending()

  private fun schedule(updateId: Int, versions: List<IdeVersion>) {
    val updateInfo = UpdateInfoCache.getUpdateInfo(updateId) ?: return
    if (updateInfo in verifiableUpdates) {
      return
    }
    val lastCheckAgo = System.currentTimeMillis() - (lastCheckDate[updateInfo] ?: 0)
    if (lastCheckAgo < UPDATE_CHECK_MIN_PAUSE_MS || updatesMissingCompatibleIde.contains(updateInfo) && lastCheckAgo < UPDATE_MISSING_IDE_PAUSE_MS) {
      return
    }

    lastCheckDate[updateInfo] = System.currentTimeMillis()

    val pluginCoordinate = PluginCoordinate.ByUpdateInfo(updateInfo, ServerInstance.pluginRepository)
    val rangeRunnerParams = CheckRangeParams(JdkVersion.JAVA_8_ORACLE)
    val runner = CheckRangeCompatibilityTask(updateInfo, pluginCoordinate, rangeRunnerParams, versions, ServerInstance.pluginRepository, ServerInstance.pluginDetailsProvider)
    val taskStatus = taskManager.enqueue(
        runner,
        { taskResult -> onSuccess(taskResult, updateInfo) },
        { error, tid, task -> onError(error, tid, task) },
        { _, task -> onCompletion(task) }
    )
    verifiableUpdates.add(updateInfo)
    LOG.info("Check [since; until] for $updateInfo is scheduled #${taskStatus.taskId}")
  }

  private fun onCompletion(task: CheckRangeCompatibilityTask) {
    verifiableUpdates.remove((task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo)
  }

  private fun onError(error: Throwable, taskStatus: TaskStatus, task: CheckRangeCompatibilityTask) {
    val updateInfo = (task.pluginCoordinate as PluginCoordinate.ByUpdateInfo).updateInfo
    LOG.error("Unable to check $updateInfo (task #${taskStatus.taskId})", error)
  }

  private fun onSuccess(compatibilityResult: CheckRangeCompatibilityResult, updateInfo: UpdateInfo) {
    val ideVersionToResult = compatibilityResult.verificationResults.orEmpty().map { it.ideVersion to it.verdict.javaClass.simpleName }
    LOG.info("Update ${compatibilityResult.updateInfo} is checked: ${compatibilityResult.resultType}: ${ideVersionToResult.joinToString()}")

    if (compatibilityResult.resultType == CheckRangeCompatibilityResult.ResultType.NO_COMPATIBLE_IDES) {
      updatesMissingCompatibleIde.add(updateInfo)
    } else {
      updatesMissingCompatibleIde.remove(updateInfo)
    }

    val verificationResult = prepareVerificationResponse(compatibilityResult)

    try {
      sendUpdateCheckResult(verificationResult, pluginRepositoryUserName, pluginRepositoryPassword).executeSuccessfully()
    } catch(e: Exception) {
      LOG.error("Unable to send verification result of ${compatibilityResult.updateInfo}", e)
    }
  }

  private fun getUpdatesToCheck(availableIde: IdeVersion, userName: String, password: String) =
      getVerifierConnector().getUpdatesToCheck(createStringRequestBody(availableIde.asString()), createStringRequestBody(userName), createStringRequestBody(password))

  private fun sendUpdateCheckResult(checkResult: String, userName: String, password: String): Call<ResponseBody> =
      getVerifierConnector().sendUpdateCheckResult(createJsonRequestBody(checkResult), createStringRequestBody(userName), createStringRequestBody(password))

}