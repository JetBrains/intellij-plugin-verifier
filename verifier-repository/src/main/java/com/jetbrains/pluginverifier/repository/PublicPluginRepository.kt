package com.jetbrains.pluginverifier.repository

import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.Stopwatch
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.PluginDownloader
import com.jetbrains.pluginverifier.repository.files.FileRepositoryBuilder
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import com.jetbrains.pluginverifier.repository.files.PluginFileNameMapper
import com.jetbrains.pluginverifier.repository.retrofit.JsonUpdateInfo
import com.jetbrains.pluginverifier.repository.retrofit.JsonUpdateSinceUntil
import com.jetbrains.pluginverifier.repository.retrofit.PublicPluginRepositoryConnector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

/**
 * The plugin repository implementation that communicates with
 * [JetBrains Plugins Repository](https://plugins.jetbrains.com/)
 */
class PublicPluginRepository(private val repositoryUrl: String,
                             downloadDir: Path,
                             diskSpaceSetting: DiskSpaceSetting) : PluginRepository {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(PublicPluginRepository::class.java)

    private const val BATCH_REQUEST_SIZE = 1000

    /**
     * TODO: implement this mapping on the Plugins Repository.
     * The list of IntelliJ plugins which define some modules
     * (e.g. the plugin "org.jetbrains.plugins.ruby" defines a module "com.intellij.modules.ruby")
     */
    private val INTELLIJ_MODULE_TO_CONTAINING_PLUGIN = ImmutableMap.of(
        "com.intellij.modules.ruby", "org.jetbrains.plugins.ruby",
        "com.intellij.modules.php", "com.jetbrains.php",
        "com.intellij.modules.python", "Pythonid",
        "com.intellij.modules.swift.lang", "com.intellij.clion-swift")
  }

  //todo: move it to a separate class.
  private val downloadedPluginsFileRepository = FileRepositoryBuilder().createFromExistingFiles(
      downloadDir,
      PluginDownloader(repositoryUrl),
      PluginFileNameMapper(),
      LruFileSizeSweepPolicy(diskSpaceSetting),
      keyProvider = { PluginFileNameMapper.getUpdateIdByFile(it) }
  )

  private val repositoryConnector: PublicPluginRepositoryConnector = Retrofit.Builder()
      .baseUrl(repositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(PublicPluginRepositoryConnector::class.java)

  private var lastUpdateId = 1

  private val fullUpdateTimer = Stopwatch(Duration.ofHours(1))

  private val cache: ConcurrentMap<Int, UpdateInfo> = ConcurrentHashMap()

  private val repositoryURL = URL(repositoryUrl)

  private fun JsonUpdateInfo.addUpdateInfo() {
    cache[updateId] = toUpdateInfo()
  }

  private fun JsonUpdateInfo.toUpdateInfo() = UpdateInfo(
      pluginId,
      version,
      pluginName,
      updateId,
      vendor,
      sinceBuild,
      untilBuild,
      getDownloadUrl(updateId),
      getBrowserUrl(pluginId),
      repositoryURL
  )

  private fun JsonUpdateSinceUntil.toUpdateInfo(): UpdateInfo? {
    val updateInfo = getUpdateInfoById(updateId) ?: return null
    return with(updateInfo) {
      val downloadUrl = getDownloadUrl(updateId)
      val browserUrl = getBrowserUrl(pluginId)

      UpdateInfo(
          pluginId,
          pluginName,
          version,
          updateId,
          vendor,
          sinceBuild,
          untilBuild,
          downloadUrl,
          browserUrl,
          repositoryURL
      )
    }
  }

  private fun getBrowserUrl(pluginId: String) =
      URL("$repositoryUrl/plugin/index?xmlId=$pluginId")

  private fun getDownloadUrl(updateId: Int) =
      URL("$repositoryUrl/plugin/download/?noStatistic=true&updateId=$updateId")


  override fun getUpdateInfoById(updateId: Int): UpdateInfo? {
    if (updateId !in cache) {
      requestBatchOfUpdateInfos(updateId)
    }
    return cache[updateId]
  }

  override fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo> {
    val jsonUpdatesResponse = try {
      repositoryConnector.getUpdates(pluginId).executeSuccessfully().body()
    } catch (e: Exception) {
      return emptyList()
    }

    return with(jsonUpdatesResponse) {
      updates.map {
        with(it) {
          UpdateInfo(
              pluginId,
              updateVersion,
              pluginName,
              updateId,
              vendor,
              since,
              until,
              getDownloadUrl(updateId),
              getBrowserUrl(pluginId),
              repositoryURL
          )
        }
      }
    }
  }

  /**
   * Current implementation uses [allUpdatesSince](https://plugins.jetbrains.com/manager/allUpdatesSince?build=IU-139&updateId=40000)
   * endpoint to request all available plugins.
   * In order to reduce the load on the repository,
   * the class remembers the last requested update id.
   *
   * Because of possible changes of *since, until* values in the repository's database,
   * it is necessary to occasionally do a full
   * request (with *updateId* value of `1`) to update the caches.
   * The full request is performed every hour.
   */
  override fun getAllPlugins(): List<UpdateInfo> {
    val plugins = if (fullUpdateTimer.isCycle()) {
      val fullUpdate = requestPluginInfosSince(1)
      fullUpdateTimer.reset()
      cache.clear()
      fullUpdate
    } else {
      requestPluginInfosSince(lastUpdateId + 1)
    }
    plugins.forEach {
      cache[it.updateId] = it
    }

    lastUpdateId = Math.max(lastUpdateId, cache.map { it.value.updateId }.max() ?: 0)
    return cache.values.toList()
  }

  private fun requestPluginInfosSince(startUpdateId: Int) = requestAllUpdatesSince(startUpdateId)
      .sortedBy { it.updateId }
      .mapNotNull { it.toUpdateInfo() }


  private fun requestAllUpdatesSince(startUpdateId: Int): List<JsonUpdateSinceUntil> =
      repositoryConnector.getAllUpdateSinceAndUntil("139.0", startUpdateId).executeSuccessfully().body()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<UpdateInfo> =
      repositoryConnector.getAllCompatibleUpdates(ideVersion.asString()).executeSuccessfully().body().map { it.toUpdateInfo() }

  private fun String?.createIdeVersion(): IdeVersion? {
    if (this == null || this == "0.0") {
      return null
    }
    return IdeVersion.createIdeVersionIfValid(this)
  }

  private fun UpdateInfo.isCompatibleWith(ideVersion: IdeVersion): Boolean {
    val since = sinceBuild.createIdeVersion()
    val until = untilBuild.createIdeVersion()
    return (since == null || since <= ideVersion) && (until == null || ideVersion <= until)
  }

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> =
      getAllVersionsOfPlugin(pluginId).filter { it.isCompatibleWith(ideVersion) }

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? =
      INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]

  override fun downloadPluginFile(updateInfo: UpdateInfo): FileRepositoryResult =
      downloadedPluginsFileRepository.getFile(updateInfo.updateId)

  private fun requestBatchOfUpdateInfos(updateId: Int) {
    try {
      val fromUpdateId = (updateId - BATCH_REQUEST_SIZE / 2).coerceAtLeast(1)
      val toUpdateId = fromUpdateId + BATCH_REQUEST_SIZE
      val updates = repositoryConnector.getUpdateInfosForIdsBetween(fromUpdateId, toUpdateId)
          .executeSuccessfully().body()
      updates.forEach { it.addUpdateInfo() }
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo for #$updateId", e)
      requestSingleUpdateId(updateId)
    }
  }

  private fun requestSingleUpdateId(updateId: Int) {
    try {
      val info = repositoryConnector.getUpdateInfoById(updateId).executeSuccessfully().body()
      info.addUpdateInfo()
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo #$updateId", e)
    }
  }

  override fun toString(): String = repositoryUrl

}
