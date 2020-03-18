package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.PluginRepository
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.ResponseBody
import org.w3c.dom.Document
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * [PluginRepository] of JetBrains TeamCity Plugin for IntelliJ IDEA, which is built on TeamCity.
 */
class TeamCityIdeaPluginRepository(
  private val buildServerUrl: URL,
  private val sourceCodeUrl: URL
) : CustomPluginRepository() {

  private val repositoryConnector = Retrofit.Builder()
    .baseUrl(buildServerUrl.toHttpUrlOrNull()!!)
    .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
    .build()
    .create(TeamCityPluginRepositoryConnector::class.java)

  override val repositoryUrl: URL
    get() = buildServerUrl

  override fun requestAllPlugins(): List<CustomPluginInfo> {
    val document = repositoryConnector.getPluginsList()
      .executeSuccessfully()
      .body()!!.byteStream().use {
        DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(it)
      }
    return parsePluginsList(document, buildServerUrl, sourceCodeUrl)
  }

  override val presentableName
    get() = "JetBrains TeamCity Plugin Repository"

  override fun toString() = presentableName

  private interface TeamCityPluginRepositoryConnector {
    @GET("/update/idea-plugins.xml")
    fun getPluginsList(): Call<ResponseBody>
  }

  companion object {
    fun parsePluginsList(document: Document, buildServerUrl: URL, sourceCodeUrl: URL) =
      parsePluginsListXml(document).map {
        CustomPluginInfo(
          it.id,
          "TeamCity Integration",
          it.version,
          "JetBrains",
          URL(buildServerUrl, "/update/${it.url}"),
          buildServerUrl,
          sourceCodeUrl
        )
      }
  }

}