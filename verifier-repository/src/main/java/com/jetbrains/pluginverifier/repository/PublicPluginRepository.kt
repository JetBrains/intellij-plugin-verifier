package com.jetbrains.pluginverifier.repository

import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class PublicPluginRepository(private val repositoryUrl: String,
                             downloadDir: File,
                             downloadDirMaxSpace: Long) : PluginRepository {
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

  private val downloadManager = DownloadManager(downloadDir.createDir(), downloadDirMaxSpace, {
    repositoryApi.downloadFile(it.updateId).executeSuccessfully()
  })

  override fun getPluginOverviewUrl(pluginInfo: PluginInfo): String? = if (pluginInfo is UpdateInfo) {
    repositoryUrl + "/plugin/index?xmlId=" + pluginInfo.pluginId
  } else {
    null
  }

  override fun getUpdateInfoById(updateId: Int): UpdateInfo? {
    val call = repositoryApi.getUpdateInfoById(updateId)
    val response = call.execute()
    return if (response.isSuccessful) {
      response.body().toUpdateInfo()
    } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND || response.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
      null
    } else {
      throw RuntimeException("Unable to get update info #$updateId: ${response.code()}")
    }
  }

  override fun getAllUpdatesOfPlugin(pluginId: String): List<UpdateInfo>? {
    val call = repositoryApi.getUpdates(pluginId)
    val response = call.execute()
    return when {
      response.isSuccessful -> {
        val body = response.body()
        val pluginXmlId = body.pluginXmlId
        val name = body.pluginName
        body.updates.map { UpdateInfo(pluginXmlId, name, it.updateVersion, it.id, body.vendor) }
      }
      response.code() == HttpURLConnection.HTTP_NOT_FOUND -> null
      else -> throw RuntimeException("Unable to get updates by pluginId = $pluginId: ${response.code()}")
    }
  }

  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> =
      repositoryApi.getLastCompatibleUpdates(ideVersion.asString()).executeSuccessfully().body().map { it.toUpdateInfo() }

  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? =
      getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> =
      repositoryApi.getOriginalCompatibleUpdatesByPluginIds(ideVersion.asString(), pluginId).executeSuccessfully().body().map { it.toUpdateInfo() }

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? =
      INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]

  override fun downloadPluginFile(update: UpdateInfo): DownloadPluginResult = downloadManager.getOrDownloadPlugin(update)

  private val repositoryApi = Retrofit.Builder()
      .baseUrl(repositoryUrl + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(RetrofitRepositoryApi::class.java)

}
