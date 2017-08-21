package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.misc.executeSuccessfully
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

private interface RepositoryApi {

  @GET("/manager/getUpdateInfoById")
  fun getUpdateInfoById(@Query("updateId") updateId: Int): Call<UpdateInfo>

  @GET("/manager/allCompatibleUpdates")
  fun getLastCompatibleUpdates(@Query("build") build: String): Call<List<UpdateInfo>>

  @GET("/manager/originalCompatibleUpdatesByPluginIds")
  fun getOriginalCompatibleUpdatesByPluginIds(@Query("build") build: String, @Query("pluginIds") pluginId: String): Call<List<UpdateInfo>>

}

object RepositoryManager : PluginRepository {

  override fun getUpdateInfoById(updateId: Int): UpdateInfo? {
    val call = repositoryApi.getUpdateInfoById(updateId)
    val response = call.execute()
    if (response.isSuccessful) {
      return response.body()
    } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND || response.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
      return null
    } else {
      throw RuntimeException("Unable to get update info #$updateId: ${response.code()}")
    }
  }

  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> =
      repositoryApi.getLastCompatibleUpdates(ideVersion.asString()).executeSuccessfully().body()

  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? =
      getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> =
      repositoryApi.getOriginalCompatibleUpdatesByPluginIds(ideVersion.asString(), pluginId).executeSuccessfully().body()

  override fun getPluginFile(update: UpdateInfo): FileLock? = DownloadManager.getOrLoadUpdate(update)

  private val repositoryApi: RepositoryApi = Retrofit.Builder()
      .baseUrl(RepositoryConfiguration.pluginRepositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(RepositoryApi::class.java)

}
