package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.File
import java.io.PrintWriter
import java.util.jar.JarFile

object IdeResourceUtil {

  /**
   * Reads content of `<idePath>/lib/resources.jar/brokenPlugins.txt`.
   */
  private fun readBrokenPluginsTxt(idePath: File): List<String>? {
    val jarFile = idePath.resolve("lib").resolve("resources.jar")
    if (jarFile.exists()) {
      JarFile(jarFile).use {
        val jarEntry = it.getJarEntry("brokenPlugins.txt") ?: return null
        return it.getInputStream(jarEntry).bufferedReader().readLines()
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

  private fun getBrokenPluginsByLines(lines: List<String>): Set<PluginIdAndVersion> =
      lines
          .map { line -> line.trim() }
          .filterNot { it.startsWith("//") }
          .flatMapTo(hashSetOf()) { getBrokenPluginsByLine(it) }

  fun dumbBrokenPluginsList(dumpBrokenPluginsFile: File, brokenPlugins: List<PluginInfo>) {
    PrintWriter(dumpBrokenPluginsFile.create()).use { out ->
      out.println(
          "// This file contains list of broken plugins.\n" +
              "// Each line contains plugin ID and list of versions that are broken.\n" +
              "// If plugin name or version contains a space you can quote it like in command line.\n"
      )

      brokenPlugins.groupBy { it.pluginId }.forEach {
        out.print(ParametersListUtil.join(listOf(it.key)))
        out.print("    ")
        out.println(ParametersListUtil.join(it.value.map { it.version }.sortedWith(VersionComparatorUtil.COMPARATOR)))
      }
    }
  }

  /**
   * Reads set of plugins from [file], which is in form of
   * lines starting with pluginId followed by its versions
   * via whitespace
   * ```
   * pluginId_one v1 v2 v3
   * pluginId_two v1 v5
   * ```
   */
  fun readBrokenPluginsFromFile(file: File): Set<PluginIdAndVersion> =
      file.bufferedReader().use {
        getBrokenPluginsByLines(it.readLines())
      }

  /**
   * Returns set of plugins marked as "broken" for this [ide]
   * in file `<IDE>/lib/resources.jar/brokenPlugins.txt`.
   *
   * This option is used to warn a user if a plugin may cause IDE startup or
   * other errors, and this plugin had been installed in a previous
   * version of IDE (to <config>/plugins directory) and then
   * the IDE was updated.
   */
  fun getBrokenPlugins(ide: Ide): Set<PluginIdAndVersion> {
    val ideResourceFile = readBrokenPluginsTxt(ide.idePath) ?: return emptySet()
    return getBrokenPluginsByLines(ideResourceFile)
  }

}