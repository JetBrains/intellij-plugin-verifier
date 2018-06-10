package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.File
import java.io.PrintWriter
import java.util.jar.JarFile

object IdeResourceUtil {

  private val RESOURCES_JAR_PATH = "lib" + File.separator + "resources.jar"

  private const val BROKEN_PLUGINS_FILE_NAME = "brokenPlugins.txt"

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
    require(tokens.size > 1) { "The line contains plugin id, but doesn't contain versions: $line" }
    val pluginId = tokens[0]
    return tokens.drop(1).map { PluginIdAndVersion(pluginId, it) }
  }

  private fun getBrokenPluginsByLines(lines: List<String>) =
      lines
          .map { line -> line.trim { it <= ' ' } }
          .filterNot { it.startsWith("//") }
          .flatMapTo(hashSetOf()) { getBrokenPluginsByLine(it) }

  fun dumbBrokenPluginsList(dumpBrokenPluginsFile: File, brokenPlugins: List<PluginInfo>) {
    PrintWriter(dumpBrokenPluginsFile.create()).use { out ->
      out.println("// This file contains list of broken plugins.\n" +
          "// Each line contains plugin ID and list of versions that are broken.\n" +
          "// If plugin name or version contains a space you can quote it like in command line.\n")

      brokenPlugins.groupBy { it.pluginId }.forEach {
        out.print(ParametersListUtil.join(listOf(it.key)))
        out.print("    ")
        out.println(ParametersListUtil.join(it.value.map { it.version }.sortedWith(VersionComparatorUtil.COMPARATOR)))
      }
    }
  }

  fun readBrokenPluginsFromFile(file: File) =
      file.bufferedReader().use {
        getBrokenPluginsByLines(it.readLines())
      }

  /**
   * Returns contents of the `<IDE>/lib/resources/brokenPlugins.txt` file
   * that is a set of plugins that must not be installed in the IDE.
   */
  fun getBrokenPluginsListedInIde(ide: Ide): Set<PluginIdAndVersion> {
    val ideResourceFile = readIdeResourceLines(ide, RESOURCES_JAR_PATH, BROKEN_PLUGINS_FILE_NAME) ?: return emptySet()
    return getBrokenPluginsByLines(ideResourceFile)
  }

}