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
 * [PluginRepository] of Exception Analyzer plugin for IntelliJ IDEA,
 * which is available on https://ea-engine.labs.intellij.net/plugins.xml
 *
 * The plugin can be downloaded on https://ea-engine.labs.intellij.net/ExceptionAnalyzer.zip
 */
class ExceptionAnalyzerPluginRepository : CustomPluginRepository() {

  private companion object {
    val EA_PLUGIN_WEB_URL = URL("https://ea-engine.labs.intellij.net")
    val EA_PLUGIN_SOURCE_CODE_URL = URL("http://git.labs.intellij.net/?p=idea/exa.git;a=shortlog;h=refs/heads/master")
  }

  private val repositoryConnector = Retrofit.Builder()
      .baseUrl(HttpUrl.get(EA_PLUGIN_WEB_URL))
      .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(ExceptionAnalyzerRepositoryConnector::class.java)

  override fun requestAllPlugins(): List<CustomPluginInfo> {
    val document = repositoryConnector.getPluginsList()
        .executeSuccessfully()
        .body().byteStream().use {
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(it)
        }
    return parsePluginsList(document)
  }

  private fun parsePluginsList(document: Document) =
      parsePluginsListXml(document).map {
        CustomPluginInfo(
            it.id,
            "ExceptionAnalyzer",
            it.version,
            "JetBrains",
            URL(EA_PLUGIN_WEB_URL, '/' + it.url),
            EA_PLUGIN_WEB_URL,
            EA_PLUGIN_SOURCE_CODE_URL
        )
      }

  override fun toString() = "ExceptionAnalyzer Plugin Repository $EA_PLUGIN_WEB_URL"

  private interface ExceptionAnalyzerRepositoryConnector {
    @GET("/plugins.xml")
    fun getPluginsList(): Call<ResponseBody>
  }

}