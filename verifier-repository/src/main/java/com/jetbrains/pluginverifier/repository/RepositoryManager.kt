package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.executeSuccessfully
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.http.annotation.ThreadSafe
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

private interface RepositoryApi {

  @GET("/manager/getUpdateInfoById")
  fun getUpdateInfoById(@Query("updateId") updateId: Int): Call<UpdateInfo>

  @GET("/manager/allCompatibleUpdates")
  fun getLastCompatibleUpdates(@Query("build") build: String): Call<List<UpdateInfo>>

  @GET("/manager/originalCompatibleUpdatesByPluginIds")
  fun getOriginalCompatibleUpdatesByPluginIds(@Query("build") build: String, @Query("pluginIds") pluginId: String): Call<List<UpdateInfo>>

}

@ThreadSafe
object RepositoryManager : PluginRepository {

  override fun getUpdateInfoById(updateId: Int): UpdateInfo {
    return repositoryApi.getUpdateInfoById(updateId).executeSuccessfully().body()
  }

  @Throws(IOException::class)
  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> {
    LOG.debug("Loading list of plugins compatible with $ideVersion... ")

    val updates = repositoryApi.getLastCompatibleUpdates(ideVersion.asString())
    return updates.executeSuccessfully().body()
  }

  @Throws(IOException::class)
  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? {
    LOG.debug("Fetching last compatible update of plugin {} with ide {}", pluginId, ideVersion)

    //search the given number in the all compatible updates
    val all = getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId)
    var result: UpdateInfo? = null
    for (info in all) {
      if (result == null || result.updateId < info.updateId) {
        result = info
      }
    }

    return result
  }

  @Throws(IOException::class)
  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> {
    LOG.debug("Fetching list of all compatible builds of a pluginId $pluginId on IDE $ideVersion")

    val call = repositoryApi.getOriginalCompatibleUpdatesByPluginIds(ideVersion.asString(), pluginId)
    return call.executeSuccessfully().body()
  }

  @Throws(IOException::class)
  override fun getPluginFile(update: UpdateInfo): FileLock? {
    return getPluginFile(update.updateId)
  }

  @Throws(IOException::class)
  override fun getPluginFile(updateId: Int): FileLock? {
    return DownloadManager.getOrLoadUpdate(updateId)
  }

  private val LOG = LoggerFactory.getLogger(RepositoryManager::class.java)

  private val repositoryApi: RepositoryApi = Retrofit.Builder()
      .baseUrl(RepositoryConfiguration.pluginRepositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeClient())
      .build()
      .create(RepositoryApi::class.java)

  private fun makeClient(): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.MINUTES)
      .readTimeout(5, TimeUnit.MINUTES)
      .writeTimeout(5, TimeUnit.MINUTES)
      .addInterceptor(HttpLoggingInterceptor().setLevel(if (LOG.isDebugEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
      .build()

}
