package com.jetbrains.pluginverifier.repository

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
import com.jetbrains.pluginverifier.repository.retrofit.PublicPluginRepositoryConnector
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit
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

  private val repositoryURL = URL(repositoryUrl)

  private val allSinceUntilPluginsRequester = AllSinceUntilPluginsRequester()

  private val allJsonUpdateInfosRequester = JsonUpdateInfosRequester()

  private fun getBrowserUrl(pluginId: String) = URL("$repositoryUrl/plugin/index?xmlId=$pluginId")

  private fun getDownloadUrl(updateId: Int) = URL("$repositoryUrl/plugin/download/?noStatistic=true&updateId=$updateId")

  override fun getUpdateInfoById(updateId: Int): UpdateInfo? {
    val sinceUntil = allSinceUntilPluginsRequester.getSinceUntilById(updateId) ?: return null
    val updateInfo = allJsonUpdateInfosRequester.getJsonUpdateInfoById(updateId) ?: return null
    return createUpdateInfo(updateInfo, sinceUntil)
  }

  override fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo> {
    val jsonUpdatesResponse = try {
      repositoryConnector
          .getPluginUpdates(pluginId)
          .executeSuccessfully().body()
    } catch (e: Exception) {
      return emptyList()
    }
    return jsonUpdatesResponse.updates.mapNotNull { getUpdateInfoById(it.updateId) }
  }

  override fun getAllPlugins() = allSinceUntilPluginsRequester.getAllPlugins()
      .mapNotNull { getUpdateInfoById(it.updateId) }

  private fun createUpdateInfo(jsonUpdateInfo: JsonUpdateInfo, sinceUntil: JsonUpdateSinceUntil) =
      with(jsonUpdateInfo) {
        UpdateInfo(
            pluginId,
            version,
            pluginName,
            updateId,
            version,
            sinceUntil.sinceBuild,
            sinceUntil.untilBuild,
            getDownloadUrl(updateId),
            getBrowserUrl(pluginId),
            repositoryURL
        )
      }

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      repositoryConnector.getAllCompatibleUpdates(ideVersion.asString())
          .executeSuccessfully().body()
          .mapNotNull { getUpdateInfoById(it.updateId) }

  private fun String?.createRangeIdeVersion(): IdeVersion? {
    if (this == null || this == "" || this == "0.0") {
      return null
    }
    return IdeVersion.createIdeVersionIfValid(this)
  }

  private fun UpdateInfo.isCompatibleWith(ideVersion: IdeVersion): Boolean {
    val since = sinceBuild.createRangeIdeVersion()
    val until = untilBuild.createRangeIdeVersion()
    return (since == null || since <= ideVersion) && (until == null || ideVersion <= until)
  }

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllVersionsOfPlugin(pluginId).filter { it.isCompatibleWith(ideVersion) }

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
      INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]

  override fun downloadPluginFile(updateInfo: UpdateInfo) =
      downloadedPluginsFileRepository.getFile(updateInfo.updateId)

  override fun toString() = repositoryUrl

  /**
   * This class is responsible for requesting
   * the general plugin information without the [since; until] ranges.
   */
  private inner class JsonUpdateInfosRequester {

    private val jsonUpdates = hashMapOf<Int, JsonUpdateInfo>()

    private val requestedUpdateIds = hashSetOf<Int>()

    private fun requestBatchOfJsonUpdateInfos(updateId: Int) = try {
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

    @Synchronized
    fun getJsonUpdateInfoById(updateId: Int): JsonUpdateInfo? {
      if (updateId !in requestedUpdateIds) {
        requestedUpdateIds.add(updateId)

        val jsonUpdateInfos = requestBatchOfJsonUpdateInfos(updateId)
        val updateIds = jsonUpdateInfos.map { it.updateId }
        if (updateIds.isNotEmpty()) {
          /**
           * Don't request the same range of update infos twice.
           */
          val min = updateIds.min()!!
          val max = updateIds.max()!!
          requestedUpdateIds.addAll(min..max)

          jsonUpdateInfos.forEach {
            jsonUpdates[it.updateId] = it
          }
        }
      }
      return jsonUpdates[updateId]
    }

  }

  /**
   * This class is responsible for requesting all available
   * plugins informations along with the [since; until] ranges
   * of the plugins.
   *
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
  private inner class AllSinceUntilPluginsRequester {

    private val duration = Duration.of(1, ChronoUnit.HOURS)

    private var lastUpdateId = 1

    private var lastFullUpdate: Instant? = null

    private val allPlugins = hashMapOf<Int, JsonUpdateSinceUntil>()

    private fun needFullUpdate() = lastFullUpdate == null || now().minus(duration).isAfter(lastFullUpdate)

    private fun updateState() {
      val updates = if (needFullUpdate()) {
        val fullUpdate = requestSinceUntil(1)
        lastFullUpdate = now()
        fullUpdate
      } else {
        requestSinceUntil(lastUpdateId + 1)
      }

      updates.forEach {
        allPlugins[it.updateId] = it
        lastUpdateId = maxOf(lastUpdateId, it.updateId)
      }
    }

    private fun requestSinceUntil(startUpdateId: Int) = requestAllUpdatesSince(startUpdateId)
        .sortedBy { it.updateId }

    private fun requestAllUpdatesSince(startUpdateId: Int) =
        repositoryConnector.getAllUpdateSinceAndUntil("1.0", startUpdateId).executeSuccessfully().body()

    @Synchronized
    fun getAllPlugins(): Collection<JsonUpdateSinceUntil> {
      updateState()
      return allPlugins.values
    }

    @Synchronized
    fun getSinceUntilById(updateId: Int): JsonUpdateSinceUntil? {
      if (updateId > lastUpdateId || needFullUpdate()) {
        updateState()
      }
      return allPlugins[updateId]
    }
  }

}
