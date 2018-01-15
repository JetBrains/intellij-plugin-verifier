package com.jetbrains.pluginverifier.repository

import com.google.common.cache.CacheBuilder
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.PluginDownloader
import com.jetbrains.pluginverifier.repository.files.FileRepositoryBuilder
import com.jetbrains.pluginverifier.repository.files.PluginFileNameMapper
import com.jetbrains.pluginverifier.repository.retrofit.JsonUpdateInfo
import com.jetbrains.pluginverifier.repository.retrofit.JsonUpdateSinceUntil
import com.jetbrains.pluginverifier.repository.retrofit.JsonUpdatesResponse
import com.jetbrains.pluginverifier.repository.retrofit.PublicPluginRepositoryConnector
import okhttp3.HttpUrl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The plugin repository implementation that communicates with
 * [JetBrains Plugins Repository](https://plugins.jetbrains.com/)
 */
class PublicPluginRepository(override val repositoryURL: URL,
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

  private val downloadedPluginsFileRepository = FileRepositoryBuilder().createFromExistingFiles(
      downloadDir,
      PluginDownloader(repositoryURL),
      PluginFileNameMapper(),
      LruFileSizeSweepPolicy(diskSpaceSetting),
      keyProvider = { PluginFileNameMapper.getUpdateIdByFile(it) },
      presentableName = "downloaded-plugins"
  )

  private val repositoryConnector: PublicPluginRepositoryConnector = Retrofit.Builder()
      .baseUrl(HttpUrl.get(repositoryURL))
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(PublicPluginRepositoryConnector::class.java)

  private val allSinceUntilPluginsRequester = AllSinceUntilPluginsRequester()

  private val updateInfosRequester = UpdateInfosRequester()

  private fun getBrowserUrl(pluginId: String) = URL("${repositoryURL.toExternalForm().trimEnd('/')}/plugin/index?xmlId=$pluginId")

  private fun getDownloadUrl(updateId: Int) = URL("${repositoryURL.toExternalForm().trimEnd('/')}/plugin/download/?noStatistic=true&updateId=$updateId")

  override fun getPluginInfoById(updateId: Int) =
      updateInfosRequester.getUpdateInfoById(updateId)

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  override fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo> {
    val jsonUpdatesResponse = try {
      repositoryConnector
          .getPluginUpdates(pluginId)
          .executeSuccessfully().body()
    } catch (e: Exception) {
      return emptyList()
    }
    return jsonUpdatesResponse.registerUpdateInfos()
  }

  private fun JsonUpdatesResponse.registerUpdateInfos(): List<UpdateInfo> {
    val updateInfos = updates.map {
      UpdateInfo(
          pluginId,
          pluginName,
          it.updateVersion,
          this@PublicPluginRepository,
          it.sinceBuild.prepareIdeVersion(),
          it.untilBuild.prepareIdeVersion(),
          vendor,
          it.updateId,
          getDownloadUrl(it.updateId), getBrowserUrl(pluginId)
      )
    }
    updateInfos.forEach { updateInfosRequester.putUpdateInfo(it) }
    return updateInfos
  }

  private fun String?.prepareIdeVersion(): IdeVersion? =
      if (this == null || this == "" || this == "0.0") {
        null
      } else {
        IdeVersion.createIdeVersionIfValid(this)
      }

  override fun getAllPlugins() = allSinceUntilPluginsRequester.getAllPluginUpdateIds()
      .mapNotNull { getPluginInfoById(it) }

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      repositoryConnector.getAllCompatibleUpdates(ideVersion.asString())
          .executeSuccessfully().body()
          .map { updateInfosRequester.putJsonUpdateInfo(it) }

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllVersionsOfPlugin(pluginId).filter { it.isCompatibleWith(ideVersion) }

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
      INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]

  override fun downloadPluginFile(pluginInfo: PluginInfo) =
      downloadedPluginsFileRepository.getFile((pluginInfo as UpdateInfo).updateId)

  override fun toString(): String = repositoryURL.toExternalForm()

  /**
   * This class is responsible for requesting the [UpdateInfo]s.
   */
  private inner class UpdateInfosRequester {

    /**
     * Because of possible changes of *since, until*
     * values in the repository's database,
     * it is necessary to occasionally update the caches.
     * Thus, the values are invalidated every hour.
     *
     * The value of the cache is `Optional` because
     * the guava's caches don't allow to cache `null` values.
     */
    private val updateInfos = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<Int, Optional<UpdateInfo>>()

    //synchronized block is used here to avoid
    //performing unnecessary batch requests
    @Synchronized
    fun getUpdateInfoById(updateId: Int): UpdateInfo? = updateInfos.get(updateId) {
      requestUpdateInfos(updateId)
    }.orElse(null)

    @Synchronized
    fun putJsonUpdateInfo(jsonUpdateInfo: JsonUpdateInfo): UpdateInfo {
      val updateInfo = jsonUpdateInfo.toUpdateInfo()
      updateInfos.put(updateInfo.updateId, Optional.of(updateInfo))
      return updateInfo
    }

    @Synchronized
    fun putUpdateInfo(updateInfo: UpdateInfo): UpdateInfo {
      updateInfos.put(updateInfo.updateId, Optional.of(updateInfo))
      return updateInfo
    }

    private fun requestUpdateInfos(updateId: Int): Optional<UpdateInfo> {
      val jsonUpdateInfos = requestBatchOfJsonUpdateInfos(updateId)
      val updateIdToInfo = jsonUpdateInfos.associateBy { it.updateId }
      if (updateIdToInfo.isNotEmpty()) {
        val min = updateIdToInfo.keys.min()!!
        val max = updateIdToInfo.keys.max()!!
        /**
         * Explicitly save `null` value for those update-ids
         * that don't have a corresponding UpdateInfo.
         *
         * This is done to avoid unnecessary request.
         */
        for (id in min..max) {
          updateInfos.put(id, optionalUpdateInfo(updateIdToInfo[id]))
        }
      }
      return optionalUpdateInfo(updateIdToInfo[updateId])
    }

    private fun optionalUpdateInfo(jsonUpdateInfo: JsonUpdateInfo?): Optional<UpdateInfo> =
        Optional.ofNullable(jsonUpdateInfo?.toUpdateInfo())

    private fun requestBatchOfJsonUpdateInfos(updateId: Int): List<JsonUpdateInfo> = try {
      val start = (updateId - BATCH_REQUEST_SIZE / 2).coerceAtLeast(1)
      val end = start + BATCH_REQUEST_SIZE

      repositoryConnector
          .getUpdateInfosForIdsBetween(start, end)
          .executeSuccessfully().body()

    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo for #$updateId", e)
      requestSingleUpdateId(updateId).singletonOrEmpty()
    }

    private fun requestSingleUpdateId(updateId: Int) = try {
      repositoryConnector
          .getUpdateInfoById(updateId)
          .executeSuccessfully().body()
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo #$updateId", e)
      null
    }

    private fun JsonUpdateInfo.toUpdateInfo() = UpdateInfo(
        pluginId,
        pluginName,
        version,
        this@PublicPluginRepository,
        sinceString.prepareIdeVersion(),
        untilString.prepareIdeVersion(),
        vendor,
        updateId,
        getDownloadUrl(updateId), getBrowserUrl(pluginId)
    )

  }

  /**
   * This class is responsible for requesting all available
   * plugins information.
   *
   * Current implementation uses [allUpdatesSince](https://plugins.jetbrains.com/manager/allUpdatesSince?build=IU-139&updateId=40000)
   * endpoint to request all available plugins.
   * In order to reduce the load on the repository,
   * the class remembers the last requested update id.
   *
   * It is necessary to occasionally update the cashes
   * of this class because the plugins data may be changed
   * in the repository database: the "since-until" compatibility
   * ranges may be changed, or new plugins get approved.
   * We update the caches every hour.
   */
  private inner class AllSinceUntilPluginsRequester {

    private val duration = Duration.of(1, ChronoUnit.HOURS)

    private var lastUpdateId = 0

    private val allPlugins = hashMapOf<Int, JsonUpdateSinceUntil>()

    private var fullUpdateTime: Instant? = null

    private fun updateState() {
      val updates = if (needFullUpdate()) {
        fullUpdateTime = Instant.now()
        requestAllPlugins(1)
      } else {
        requestAllPlugins(lastUpdateId + 1)
      }

      updates.forEach {
        allPlugins[it.updateId] = it
        lastUpdateId = maxOf(lastUpdateId, it.updateId)
      }
    }

    private fun needFullUpdate() =
        fullUpdateTime == null || fullUpdateTime!! <= Instant.now().minus(duration)

    private fun requestAllPlugins(startUpdateId: Int) =
        repositoryConnector.getAllUpdateSinceAndUntil("1.0", startUpdateId).executeSuccessfully().body()

    @Synchronized
    fun getAllPluginUpdateIds(): Set<Int> {
      updateState()
      return allPlugins.values.mapTo(hashSetOf()) { it.updateId }
    }

  }

}
