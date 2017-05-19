package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.executeSuccessfully
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.http.annotation.ThreadSafe
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
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

  override fun getUpdateInfoById(updateId: Int): UpdateInfo =
      repositoryApi.getUpdateInfoById(updateId).executeSuccessfully().body()

  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> =
      repositoryApi.getLastCompatibleUpdates(ideVersion.asString()).executeSuccessfully().body()

  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? =
      getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> =
      repositoryApi.getOriginalCompatibleUpdatesByPluginIds(ideVersion.asString(), pluginId).executeSuccessfully().body()

  override fun getPluginFile(update: UpdateInfo): FileLock? = getPluginFile(update.updateId)

  override fun getPluginFile(updateId: Int): FileLock? = DownloadManager.getOrLoadUpdate(updateId)

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
      .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE))
      .build()

}
