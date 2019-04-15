package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.PluginRepository
import okhttp3.ResponseBody
import org.w3c.dom.Document
import org.w3c.dom.Node
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * [PluginRepository] for Multi-Push Git plugin, which is hosted in TeamCity build configuration.
 */
class MultiPushPluginRepository(private val buildConfigurationUrl: URL, private val sourceCodeUrl: URL) : CustomPluginRepository() {

  private val repositoryConnector by lazy {
    Retrofit.Builder()
        .baseUrl("https://unused.com")
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(MultiPushPluginRepositoryConnector::class.java)
  }

  override val repositoryUrl: URL
    get() = buildConfigurationUrl

  override fun requestAllPlugins(): List<CustomPluginInfo> {
    val pluginsXmlUrl = buildConfigurationUrl.toExternalForm().trimEnd('/') + "/.lastSuccessful/plugins.xml"
    val document = repositoryConnector
        .getPluginsList(pluginsXmlUrl).executeSuccessfully()
        .body().byteStream().use {
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(it)
        }
    return parsePluginsList(document, buildConfigurationUrl, sourceCodeUrl)
  }

  override fun toString() = "MultiPush Plugin Repository"

  private interface MultiPushPluginRepositoryConnector {
    @GET
    fun getPluginsList(@Url url: String): Call<ResponseBody>
  }

  companion object {
    internal fun parsePluginsList(document: Document, buildConfigurationUrl: URL, sourceCodeUrl: URL): List<CustomPluginInfo> {
      document.normalize()
      val documentElement = document.documentElement
      val nodeList = documentElement.getElementsByTagName("idea-plugin")

      val result = arrayListOf<CustomPluginInfo>()
      for (i in 0 until nodeList.length) {
        val ideaPlugin = nodeList.item(i)

        if (ideaPlugin.nodeType == Node.ELEMENT_NODE) {
          val childNodes = ideaPlugin.childNodes
          val children = (0 until childNodes.length).map { childNodes.item(it) }
          val id = children.find { it.nodeName == "id" }?.textContent?.trim() ?: continue
          val version = children.find { it.nodeName == "version" }?.textContent?.trim() ?: continue
          val downloadStr = children.find { it.nodeName == "download-url" }?.textContent?.trim() ?: continue

          val ideaVersion = children.find { it.nodeName == "idea-version" }
          val attributes = ideaVersion?.attributes

          val sinceBuild = attributes?.getNamedItem("since-build")?.textContent?.let { IdeVersion.createIdeVersionIfValid(it) }
          val untilBuild = attributes?.getNamedItem("until-build")?.textContent?.let { IdeVersion.createIdeVersionIfValid(it) }

          val downloadUrl = URL(buildConfigurationUrl.toExternalForm().trimEnd('/') + "/.lastSuccessful/$downloadStr")

          if (id.isNotEmpty() && version.isNotEmpty() && downloadStr.isNotEmpty()) {
            result += CustomPluginInfo(
                id,
                "Vcs Hosting Multi-Push",
                version,
                "JetBrains",
                downloadUrl,
                buildConfigurationUrl,
                sourceCodeUrl,
                sinceBuild,
                untilBuild
            )
          }
        }
      }
      return result
    }
  }

}