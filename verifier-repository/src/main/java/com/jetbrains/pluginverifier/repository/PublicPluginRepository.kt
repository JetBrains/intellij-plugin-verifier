package com.jetbrains.pluginverifier.repository

import com.google.common.cache.CacheBuilder
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.retrofit.JsonUpdateInfo
import com.jetbrains.pluginverifier.repository.retrofit.PublicPluginRepositoryConnector
import okhttp3.HttpUrl
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The plugin repository implementation that communicates with
 * [JetBrains Plugins Repository](https://plugins.jetbrains.com/)
 */
class PublicPluginRepository(override val repositoryURL: URL) : PluginRepository {

  companion object {
    private val LOG = LoggerFactory.getLogger(PublicPluginRepository::class.java)

    private const val DEFAULT_BATCH_REQUEST_SIZE = 1000

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

  private val repositoryConnector = Retrofit.Builder()
      .baseUrl(HttpUrl.get(repositoryURL))
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(PublicPluginRepositoryConnector::class.java)

  private val allUpdateIdsRequester = AllUpdateIdsRequester()

  private val updateInfosRequester = UpdateInfosRequester()

  private fun getBrowserUrl(pluginId: String) = URL("${repositoryURL.toExternalForm().trimEnd('/')}/plugin/index?xmlId=$pluginId")

  private fun getDownloadUrl(updateId: Int) = URL("${repositoryURL.toExternalForm().trimEnd('/')}/plugin/download/?noStatistic=true&updateId=$updateId")

  /**
   * Given the [update ID] [updateId], which is a unique identifier
   * of the plugin version in the Plugins Repository database,
   * returns its more detailed [UpdateInfo].
   */
  fun getPluginInfoById(updateId: Int) =
      updateInfosRequester.getUpdateInfoById(updateId, DEFAULT_BATCH_REQUEST_SIZE)

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  override fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo> =
      try {
        repositoryConnector
            .getPluginUpdates(pluginId).executeSuccessfully().body()
            .updateIds
            .mapNotNull { updateInfosRequester.getUpdateInfoById(it.updateId, DEFAULT_BATCH_REQUEST_SIZE) }
      } catch (e: Exception) {
        emptyList()
      }

  override fun getAllPlugins() =
      allUpdateIdsRequester.getAllUpdateIds()
          .mapNotNull { getPluginInfoById(it) }

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      repositoryConnector.getAllCompatibleUpdates(ideVersion.asString())
          .executeSuccessfully().body()
          .map { updateInfosRequester.putJsonUpdateInfo(it) }

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllVersionsOfPlugin(pluginId).filter { it.isCompatibleWith(ideVersion) }

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
      INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]

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
    //performing unnecessary interleaving batch requests
    @Synchronized
    fun getUpdateInfoById(updateId: Int, batchSize: Int): UpdateInfo? =
        updateInfos
            .get(updateId) { requestUpdateInfo(updateId, batchSize) }
            .orElse(null)

    @Synchronized
    fun putJsonUpdateInfo(jsonUpdateInfo: JsonUpdateInfo): UpdateInfo {
      val updateInfo = jsonUpdateInfo.toUpdateInfo()
      return putUpdateInfo(updateInfo)
    }

    private fun putUpdateInfo(updateInfo: UpdateInfo): UpdateInfo {
      updateInfos.put(updateInfo.updateId, Optional.of(updateInfo))
      return updateInfo
    }

    private fun requestUpdateInfo(updateId: Int, batchSize: Int): Optional<UpdateInfo> {
      val batchUpdateInfos = requestBatchOfUpdateInfos(updateId, batchSize)
      val updateIdToInfo = batchUpdateInfos.associateBy { it.updateId }
      if (updateIdToInfo.isNotEmpty()) {
        val min = updateIdToInfo.keys.min()!!
        val max = updateIdToInfo.keys.max()!!
        /**
         * Explicitly save `null` value for those update IDs
         * that don't have a corresponding UpdateInfo.
         *
         * This is done to avoid unnecessary request.
         */
        for (id in min..max) {
          updateInfos.put(id, Optional.ofNullable(updateIdToInfo[id]))
        }
      }
      return Optional.ofNullable(updateIdToInfo[updateId])
    }

    /**
     * Returns inclusive endings of a batch of [UpdateInfo]s to be requested in one request.
     */
    private fun getBatchEndings(updateId: Int, batchSize: Int): Pair<Int, Int> {
      val start = (updateId - (batchSize - 1) / 2).coerceAtLeast(1)
      val end = updateId + batchSize / 2
      return start to end
    }

    /**
     * Requests [UpdateInfo]s for [updateId] and its neighbours.
     */
    private fun requestBatchOfUpdateInfos(updateId: Int, batchSize: Int): List<UpdateInfo> {
      val (start, end) = getBatchEndings(updateId, batchSize)
      return try {
        repositoryConnector
            .getUpdateInfosForIdsBetween(start, end)
            .executeSuccessfully().body()
            .map { it.toUpdateInfo() }
      } catch (e: Exception) {
        LOG.error("Unable to request [$start; $end] UpdateInfos", e)
        requestSingleUpdateInfo(updateId).singletonOrEmpty()
      }
    }

    private fun requestSingleUpdateInfo(updateId: Int): UpdateInfo? =
        try {
          repositoryConnector
              .getUpdateInfoById(updateId)
              .executeSuccessfully().body()
              .toUpdateInfo()
        } catch (e: Exception) {
          LOG.error("Unable to request UpdateInfo #$updateId", e)
          null
        }

    private fun String?.prepareIdeVersion(): IdeVersion? =
        if (this == null || this == "" || this == "0.0") {
          null
        } else {
          IdeVersion.createIdeVersionIfValid(this)
        }

    private fun JsonUpdateInfo.toUpdateInfo() = UpdateInfo(
        pluginId,
        pluginName,
        version,
        this@PublicPluginRepository,
        sinceString.prepareIdeVersion(),
        untilString.prepareIdeVersion(),
        vendor,
        getDownloadUrl(updateId),
        updateId,
        getBrowserUrl(pluginId),
        tags.orEmpty()
    )

  }

  /**
   * This class is responsible for requesting all available
   * plugins versions information as a set of IDs in database.
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
  private inner class AllUpdateIdsRequester {

    private val duration = Duration.of(1, ChronoUnit.HOURS)

    private var lastUpdateId = 0

    private val allUpdateIds = hashSetOf<Int>()

    private var fullUpdateTime: Instant? = null

    private fun updateState() {
      if (needFullUpdate()) {
        fullUpdateTime = Instant.now()
        val fullUpdateIds = requestUpdateIds(1)
        allUpdateIds.clear()
        allUpdateIds.addAll(fullUpdateIds)
      } else {
        val newUpdateIds = requestUpdateIds(lastUpdateId + 1)
        allUpdateIds.addAll(newUpdateIds)
      }
      updateLastId(allUpdateIds)
    }

    private fun updateLastId(updateIds: Set<Int>) {
      lastUpdateId = maxOf(lastUpdateId, updateIds.max() ?: 0)
    }

    private fun needFullUpdate() =
        fullUpdateTime == null || fullUpdateTime!! <= Instant.now().minus(duration)

    private fun requestUpdateIds(startUpdateId: Int) =
        repositoryConnector
            .getAllUpdateSinceAndUntil("1.0", startUpdateId).executeSuccessfully()
            .body()
            .map { it.updateId }

    @Synchronized
    fun getAllUpdateIds(): Set<Int> {
      updateState()
      return allUpdateIds
    }

  }

}
