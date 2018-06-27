package com.jetbrains.pluginverifier.repository.repositories.custom

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Represents an item in a plugins list parsed by [parsePluginsListXml].
 */
data class PluginItem(val id: String, val version: String, val url: String)

/**
 * Parses XML of plugins list, like this one for Exception Analyzer plugin:
 * ```
 * <plugins>
 *     <plugin id="com.intellij.sisyphus" url="ExceptionAnalyzer.zip" version="4.12.35"/>
 * </plugins>
 * ```
 */
fun parsePluginsListXml(document: Document): List<PluginItem> {
  document.normalize()
  val pluginsList = document.getElementsByTagName("plugin")

  val result = arrayListOf<PluginItem>()
  for (i in 0 until pluginsList.length) {
    val node = pluginsList.item(i)
    if (node.nodeType == Node.ELEMENT_NODE) {
      val element = node as? Element ?: continue
      val id = element.getAttribute("id")
      val pluginUrl = element.getAttribute("url")
      val version = element.getAttribute("version")
      if (id.isNotEmpty() && version.isNotEmpty() && pluginUrl.isNotEmpty()) {
        result.add(PluginItem(id, version, pluginUrl))
      }
    }
  }
  return result
}
