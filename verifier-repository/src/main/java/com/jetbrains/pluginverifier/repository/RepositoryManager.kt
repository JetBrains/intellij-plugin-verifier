package com.jetbrains.pluginverifier.repository

import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
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

  @GET("/plugin/updates")
  fun getUpdates(@Query("xmlId") xmlId: String): Call<UpdatesResponse>

  @GET("/manager/originalCompatibleUpdatesByPluginIds")
  fun getOriginalCompatibleUpdatesByPluginIds(@Query("build") build: String, @Query("pluginIds") pluginId: String): Call<List<UpdateInfo>>

}

private data class UpdatesResponse(val pluginXmlId: String,
                                   val pluginName: String,
                                   val vendor: String,
                                   val updates: List<Update>?) {
  data class Update(val id: Int, val updateVersion: String, val since: String?, val until: String?)
}

object RepositoryManager : PluginRepository {
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

  override fun getAllUpdatesOfPlugin(pluginId: String): List<UpdateInfo>? {
    val call = repositoryApi.getUpdates(pluginId)
    val response = call.execute()
    if (response.isSuccessful) {
      val updates = response.body()
      //TODO: get rid of this when the Repository is ready.
      if (updates.updates == null) {
        return null
      }
      val pluginXmlId = updates.pluginXmlId
      val name = updates.pluginName
      return updates.updates.map {
        UpdateInfo(pluginXmlId, name, it.updateVersion, it.id, updates.vendor)
      }
    } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
      return null
    } else {
      throw RuntimeException("Unable to get updates by pluginId = $pluginId: ${response.code()}")
    }
  }

  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> =
      repositoryApi.getLastCompatibleUpdates(ideVersion.asString()).executeSuccessfully().body()

  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? =
      getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> =
      repositoryApi.getOriginalCompatibleUpdatesByPluginIds(ideVersion.asString(), pluginId).executeSuccessfully().body()

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? =
      INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]

  override fun getPluginFile(update: UpdateInfo): FileLock? = DownloadManager.getOrLoadUpdate(update)

  private val repositoryApi: RepositoryApi = Retrofit.Builder()
      .baseUrl(RepositoryConfiguration.pluginRepositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(RepositoryApi::class.java)

}
