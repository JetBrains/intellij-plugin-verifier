package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.createByteArrayRequestBody
import com.jetbrains.pluginverifier.network.createStringRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.UpdateInfo
import okhttp3.HttpUrl
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.jdks.JdkVersion
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Verifier service is responsible for plugins bytecode verification using the *plugin verifier* tool.
 *
 * This service periodically accesses the plugin repository, fetches plugins which should be verified,
 * and sends the verification reports.
 *
 * [Plugin verifier integration with the Plugins Repository](https://confluence.jetbrains.com/display/PLREP/plugin-verifier+integration+with+the+plugins.jetbrains.com)
 */
class VerifierService(serverContext: ServerContext, repositoryUrl: URL) : BaseService("VerifierService", 0, 5, TimeUnit.MINUTES, serverContext) {

  private val verifiableUpdates = hashSetOf<UpdateInfo>()

  private val lastCheckDate = hashMapOf<UpdateInfo, Instant>()

  private val updatesMissingCompatibleIde = TreeSet<UpdateInfo>(compareBy { it.updateId })

  private val repositoryConnector: VerificationPluginRepositoryConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(repositoryUrl))
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .client(makeOkHttpClient(logger.isDebugEnabled, 5, TimeUnit.MINUTES))
        .build()
        .create(VerificationPluginRepositoryConnector::class.java)
  }

  override fun doServe() {
    val updateToIdes = requestUpdatesToCheck()
    logger.info("Checking updates: ${updateToIdes.asMap()}")
    for ((updateInfo, ideVersions) in updateToIdes.asMap()) {
      if (verifiableUpdates.size > 500) {
        return
      }
      if (updateInfo !in verifiableUpdates && !isCheckedRecently(updateInfo)) {
        schedule(updateInfo, ideVersions.toList())
      }
    }
  }

  private fun isCheckedRecently(updateInfo: UpdateInfo): Boolean {
    val lastCheckTime = lastCheckDate[updateInfo] ?: Instant.EPOCH
    val now = Instant.now()
    return lastCheckTime.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
        || updateInfo in updatesMissingCompatibleIde && lastCheckTime.plus(Duration.of(1, ChronoUnit.DAYS)).isAfter(now)
  }

  private fun requestUpdatesToCheck(): Multimap<UpdateInfo, IdeVersion> {
    val updateId2IdeVersions = LinkedHashMultimap.create<UpdateInfo, IdeVersion>()

    for (ideVersion in serverContext.ideKeeper.getAvailableIdeVersions()) {
      requestUpdatesToCheck(ideVersion).forEach { updateId ->
        updateId2IdeVersions.put(updateId, ideVersion)
      }
    }
    return updateId2IdeVersions
  }

  private fun requestUpdatesToCheck(ideVersion: IdeVersion) =
      requestUpdatesToCheck0(ideVersion)
          .sortedDescending()
          .mapNotNull { serverContext.pluginRepository.getPluginInfoById(it) }

  private fun schedule(updateInfo: UpdateInfo, versions: List<IdeVersion>) {
    lastCheckDate[updateInfo] = Instant.now()

    val task = CheckRangeTask(
        updateInfo,
        JdkVersion.JAVA_8_ORACLE,
        versions,
        serverContext
    )
    val taskStatus = serverContext.taskManager.enqueue(
        task,
        { taskResult, _ -> onSuccess(taskResult, updateInfo) },
        { error, tid -> onError(error, tid, task) },
        { onCompletion(task) }
    )
    verifiableUpdates.add(updateInfo)
    logger.info("Check [since; until] for $updateInfo is scheduled #${taskStatus.taskId}")
  }

  private fun onCompletion(task: CheckRangeTask) {
    verifiableUpdates.remove(task.updateInfo)
  }

  private fun onError(error: Throwable, taskStatus: ServiceTaskStatus, task: CheckRangeTask) {
    val updateInfo = task.updateInfo
    logger.error("Unable to check $updateInfo (task #${taskStatus.taskId})", error)
  }

  private fun onSuccess(result: CheckRangeTask.Result, updateInfo: UpdateInfo) {
    logger.info("Update ${result.updateInfo} is checked: $result")

    if (result.resultType == CheckRangeTask.Result.ResultType.NO_COMPATIBLE_IDES) {
      updatesMissingCompatibleIde.add(updateInfo)
    } else {
      updatesMissingCompatibleIde.remove(updateInfo)
    }

    try {
      sendUpdateCheckResult(result)
    } catch (e: Exception) {
      logger.error("Unable to send verification result of ${result.updateInfo}", e)
    }
  }

  private val userNameRequestBody = createStringRequestBody(serverContext.authorizationData.pluginRepositoryUserName)

  private val passwordRequestBody = createStringRequestBody(serverContext.authorizationData.pluginRepositoryPassword)

  private fun requestUpdatesToCheck0(availableIde: IdeVersion) =
      repositoryConnector.getUpdatesToCheck(
          createStringRequestBody(availableIde.asString()),
          userNameRequestBody,
          passwordRequestBody
      ).executeSuccessfully().body()

  private fun sendUpdateCheckResult(result: CheckRangeTask.Result) =
      repositoryConnector.sendUpdateCheckResult(
          createByteArrayRequestBody(result.prepareVerificationResponse().toByteArray()),
          userNameRequestBody,
          passwordRequestBody
      ).executeSuccessfully()

}