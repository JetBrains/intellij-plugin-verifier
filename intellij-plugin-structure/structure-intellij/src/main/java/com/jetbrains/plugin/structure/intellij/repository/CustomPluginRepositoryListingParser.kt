package com.jetbrains.plugin.structure.intellij.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.xml.DefaultXMLDocumentBuilderProvider
import org.w3c.dom.Element
import java.net.URL

/**
 * Utility class that parses list of plugins from one of supported listing formats
 * @see [CustomPluginRepositoryListingType].
 */
object CustomPluginRepositoryListingParser {

  data class PluginInfo(
    val pluginId: String,
    val pluginName: String,
    val version: String,
    val vendor: String?,
    val repositoryUrl: URL,
    val downloadUrl: URL,
    val browserUrl: URL,
    val sourceCodeUrl: URL?,
    val sinceBuild: IdeVersion?,
    val untilBuild: IdeVersion?
  )

  fun parseListOfPlugins(
    pluginsListXmlContent: String,
    pluginsListXmlUrl: URL,
    repositoryUrl: URL,
    listingType: CustomPluginRepositoryListingType
  ): List<PluginInfo> {
    val document = DefaultXMLDocumentBuilderProvider
      .documentBuilder()
      .parse(pluginsListXmlContent.toByteArray().inputStream())
    val root = document.documentElement
    return if (listingType == CustomPluginRepositoryListingType.SIMPLE) {
      parseSimpleList(root, pluginsListXmlUrl, repositoryUrl)
    } else {
      parsePluginRepositoryList(root, pluginsListXmlUrl, repositoryUrl)
    }
  }

  private fun parseSimpleList(
    root: Element,
    xmlUrl: URL,
    repositoryUrl: URL
  ): List<PluginInfo> {
    if (root.tagName != "plugins") return emptyList()

    val pluginsList = root.getElementsByTagName("plugin")

    val result = arrayListOf<PluginInfo>()
    for (i in 0 until pluginsList.length) {
      val pluginElement = pluginsList.item(i) as? Element ?: continue
      if (pluginElement.tagName != "plugin") continue

      val id = pluginElement.getAttributeOrNull("id") ?: continue
      val url = pluginElement.getAttributeOrNull("url") ?: continue
      val version = pluginElement.getAttributeOrNull("version") ?: continue
      if (id.isNotEmpty() && version.isNotEmpty() && url.isNotEmpty()) {
        val pluginName = pluginElement.getAttributeOrNull("name") ?: id
        val vendor = pluginElement.getAttributeOrNull("vendor")

        val downloadUrl = URL(xmlUrl, url)
        val ideaVersionElement: Element? = pluginElement.getSingleChild("idea-version")
        val (sinceBuild, untilBuild) = parseSinceAndUntil(ideaVersionElement)

        result += PluginInfo(
          id,
          pluginName,
          version,
          vendor,
          repositoryUrl,
          downloadUrl,
          repositoryUrl,
          null,
          sinceBuild,
          untilBuild
        )
      }
    }
    return result
  }

  private fun parsePluginRepositoryList(
    root: Element,
    xmlUrl: URL,
    repositoryUrl: URL
  ): List<PluginInfo> {
    if (root.tagName != "plugin-repository") return emptyList()
    val ideaPluginElements = root.getElementsByTagName("idea-plugin")
    val result = arrayListOf<PluginInfo>()
    for (i in 0 until ideaPluginElements.length) {
      val ideaPlugin = ideaPluginElements.item(i) as? Element ?: continue
      if (ideaPlugin.tagName != "idea-plugin") continue

      val name = ideaPlugin.getSingleChild("name")?.textContent ?: continue
      val id = ideaPlugin.getSingleChild("id")?.textContent ?: continue
      val version = ideaPlugin.getSingleChild("version")?.textContent ?: continue
      val vendor = ideaPlugin.getSingleChild("vendor")?.textContent ?: continue
      val downloadUrlRelative = ideaPlugin.getSingleChild("download-url")?.textContent ?: continue

      val ideaVersionElement: Element? = ideaPlugin.getSingleChild("idea-version")
      val (sinceBuild, untilBuild) = parseSinceAndUntil(ideaVersionElement)

      val downloadUrl = URL(xmlUrl, downloadUrlRelative)

      result += PluginInfo(
        id,
        name,
        version,
        vendor,
        repositoryUrl,
        downloadUrl,
        repositoryUrl,
        null,
        sinceBuild,
        untilBuild
      )
    }
    return result
  }

  private fun parseSinceAndUntil(ideaVersionElement: Element?): Pair<IdeVersion?, IdeVersion?> {
    val sinceBuild = ideaVersionElement?.getAttributeOrNull("since-build")?.let { IdeVersion.createIdeVersionIfValid(it) }
    val untilBuild = ideaVersionElement?.getAttributeOrNull("until-build")?.let { IdeVersion.createIdeVersionIfValid(it) }
    return sinceBuild to untilBuild
  }

  private fun Element.getAttributeOrNull(attributeName: String): String? {
    if (hasAttribute(attributeName)) {
      return getAttribute(attributeName)
    }
    return null
  }

  private fun Element.getSingleChild(tagName: String): Element? {
    val elements = getElementsByTagName(tagName)
    if (elements.length != 1) {
      return null
    }
    return elements.item(0) as? Element
  }

}