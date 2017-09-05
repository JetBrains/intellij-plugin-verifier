package com.jetbrains.pluginverifier.utils

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.tasks.PluginIdAndVersion
import java.io.File
import java.util.jar.JarFile

object IdeResourceUtil {

  private val RESOURCES_JAR_PATH = "lib" + File.separator + "resources.jar"

  private val BROKEN_PLUGINS_FILE_NAME = "brokenPlugins.txt"

  private val CHECKED_PLUGINS_FILE_NAME = "checkedPlugins.txt"

  private fun readIdeResourceLines(ide: Ide, jarPath: String, resourceFileName: String): List<String>? {
    val idePath = ide.idePath
    val jarFile = File(idePath, jarPath)
    if (jarFile.exists()) {
      JarFile(jarFile).use {
        val jarEntry = it.getJarEntry(resourceFileName) ?: return null
        return it.getInputStream(jarEntry).bufferedReader().use { it.readLines() }
      }
    } else {
      return null
    }
  }

  private fun getBrokenPluginsByLine(line: String): List<PluginIdAndVersion> {
    val tokens = ParametersListUtil.parse(line)
    if (tokens.isEmpty()) {
      return emptyList()
    }
    require(tokens.size > 1, { "The line contains plugin id, but doesn't contain versions: $line" })
    val pluginId = tokens[0]
    return tokens.drop(1).map { PluginIdAndVersion(pluginId, it) }
  }

  fun getBrokenPluginsByLines(lines: List<String>): List<PluginIdAndVersion> = lines
      .map { line -> line.trim { it <= ' ' } }
      .filterNot { it.startsWith("//") }
      .flatMap { getBrokenPluginsByLine(it) }

  fun getBrokenPluginsListedInBuild(ide: Ide): List<PluginIdAndVersion>? {
    val ideResourceFile = readIdeResourceLines(ide, RESOURCES_JAR_PATH, BROKEN_PLUGINS_FILE_NAME) ?: return null
    return getBrokenPluginsByLines(ideResourceFile)
  }

  fun getCheckedPluginIdsListedInBuild(ide: Ide): List<String>? =
      readIdeResourceLines(ide, RESOURCES_JAR_PATH, CHECKED_PLUGINS_FILE_NAME)

}