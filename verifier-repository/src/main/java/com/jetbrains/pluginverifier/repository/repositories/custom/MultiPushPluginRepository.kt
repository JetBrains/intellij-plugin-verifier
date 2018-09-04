package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.PluginRepository
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import org.w3c.dom.Document
import org.w3c.dom.Node
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * [PluginRepository] of Multi-Push Git plugin,
 * which is built on https://buildserver.labs.intellij.net
 *
 * Build configuration:
 * https://buildserver.labs.intellij.net/viewType.html?buildTypeId=ijplatform_master_Idea_Experiments_BuildMultiPushPlugin
 *
 * Plugin list can be obtained in:
 * https://buildserver.labs.intellij.net/guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin/.lastSuccessful/plugins.xml
 *
 * Plugin can be downloaded by similar URL:
 * https://buildserver.labs.intellij.net/guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin/.lastSuccessful/vcs-hosting-idea-multipush-1.0.7.zip
 */
class MultiPushPluginRepository(private val buildServerUrl: URL) : CustomPluginRepository() {

  private val repositoryConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(buildServerUrl))
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(MultiPushPluginRepositoryConnector::class.java)
  }

  override fun requestAllPlugins(): List<CustomPluginInfo> {
    val document = repositoryConnector
        .getPluginsList().executeSuccessfully()
        .body().byteStream().use {
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(it)
        }
    return parsePluginsList(document, buildServerUrl)
  }

  override fun toString() = "MultiPush Plugin Repository"

  private interface MultiPushPluginRepositoryConnector {
    @GET("/guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin/.lastSuccessful/plugins.xml")
    fun getPluginsList(): Call<ResponseBody>
  }

  companion object {
    private const val CONFIGURATION_PATH = "guestAuth/repository/download/ijplatform_master_Idea_Experiments_BuildMultiPushPlugin"

    fun parsePluginsList(document: Document, buildServerUrl: URL): List<CustomPluginInfo> {
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

          if (id.isNotEmpty() && version.isNotEmpty() && downloadStr.isNotEmpty()) {
            result.add(CustomPluginInfo(
                id,
                "Vcs Hosting Multi-Push",
                version,
                "JetBrains",
                URL(buildServerUrl, "$CONFIGURATION_PATH/.lastSuccessful/$downloadStr"),
                URL(buildServerUrl, CONFIGURATION_PATH),
                sinceBuild,
                untilBuild
            ))
          }
        }
      }
      return result
    }

  }


}