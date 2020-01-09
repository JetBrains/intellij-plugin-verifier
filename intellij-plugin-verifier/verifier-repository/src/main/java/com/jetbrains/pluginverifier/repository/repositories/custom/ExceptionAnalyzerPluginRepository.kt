package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.PluginRepository
import okhttp3.ResponseBody
import org.w3c.dom.Document
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * [PluginRepository] of Exception Analyzer plugin for IntelliJ IDEA.
 */
class ExceptionAnalyzerPluginRepository(
  override val repositoryUrl: URL,
  private val sourceCodeUrl: URL
) : CustomPluginRepository() {

  private val repositoryConnector = Retrofit.Builder()
    .baseUrl("https://unused.com")
    .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
    .build()
    .create(ExceptionAnalyzerRepositoryConnector::class.java)

  override fun requestAllPlugins(): List<CustomPluginInfo> {
    val pluginsXmlUrl = repositoryUrl.toExternalForm().trimEnd('/') + "/plugins.xml"
    val document = repositoryConnector.getPluginsList(pluginsXmlUrl)
      .executeSuccessfully()
      .body()!!.byteStream().use {
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
        URL(repositoryUrl.toExternalForm().trimEnd('/') + "/" + it.url),
        repositoryUrl,
        sourceCodeUrl
      )
    }

  override fun toString() = "ExceptionAnalyzer Plugin Repository: ${repositoryUrl.toExternalForm()}"

  private interface ExceptionAnalyzerRepositoryConnector {
    @GET
    fun getPluginsList(@Url url: String): Call<ResponseBody>
  }

}