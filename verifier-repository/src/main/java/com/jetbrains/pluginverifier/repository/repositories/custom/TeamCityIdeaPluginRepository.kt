package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.PluginRepository
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import org.w3c.dom.Document
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * [PluginRepository] of JetBrains TeamCity Plugin for IntelliJ IDEA,
 * which is built on https://buildserver.labs.intellij.net
 *
 * The last available plugin version can be seen in
 * https://buildserver.labs.intellij.net/update/idea-plugins.xml.
 */
class TeamCityIdeaPluginRepository(private val buildServerUrl: URL) : CustomPluginRepository() {

  private val repositoryConnector = Retrofit.Builder()
      .baseUrl(HttpUrl.get(buildServerUrl))
      .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(TeamCityPluginRepositoryConnector::class.java)

  override fun requestAllPlugins(): List<CustomPluginInfo> {
    val document = repositoryConnector.getPluginsList()
        .executeSuccessfully()
        .body().byteStream().use {
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(it)
        }
    return parsePluginsList(document, buildServerUrl)
  }

  override fun toString() = "JetBrains TeamCity Plugin Repository"

  private interface TeamCityPluginRepositoryConnector {
    @GET("/update/idea-plugins.xml")
    fun getPluginsList(): Call<ResponseBody>
  }

  companion object {
    fun parsePluginsList(document: Document, buildServerUrl: URL) =
        parsePluginsListXml(document).map {
          CustomPluginInfo(
              it.id,
              "TeamCity Integration",
              it.version,
              "JetBrains",
              URL(buildServerUrl, "/update/${it.url}"),
              buildServerUrl
          )
        }
  }

}