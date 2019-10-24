package com.jetbrains.plugin.structure.ide

data class PluginIdAndVersion(val pluginId: String, val version: String) {
  val presentableName: String
    get() = "$pluginId $version"
}

object IdeIncompatiblePluginsUtil {

  fun parseIncompatiblePluginsByLines(lines: List<String>): Set<PluginIdAndVersion> =
    lines
      .map { line -> line.trim() }
      .filterNot { it.startsWith("//") }
      .flatMapTo(hashSetOf()) { parseIncompatiblePluginsByLine(it) }

  fun dumpIncompatiblePluginsLines(brokenPlugins: List<PluginIdAndVersion>): List<String> =
    arrayListOf<String>().apply {
      add("// This file contains list of broken plugins.")
      add("// Each line contains plugin ID and list of versions that are broken.")
      add("// If plugin name or version contains a space you can quote it like in command line.")
      add("")

      brokenPlugins.groupBy { it.pluginId }.forEach { (pluginId, versions) ->
        val line = ParametersListUtil.join(listOf(pluginId)) +
          "    " +
          ParametersListUtil.join(versions.map { it.version }.sortedWith(VersionComparatorUtil.COMPARATOR))
        add(line)
      }
    }

  private fun parseIncompatiblePluginsByLine(line: String): List<PluginIdAndVersion> {
    val tokens = ParametersListUtil.parse(line)
    if (tokens.isEmpty()) {
      return emptyList()
    }
    require(tokens.size > 1) { "The line contains plugin id, but doesn't contain versions: $line" }
    val pluginId = tokens[0]
    return tokens.drop(1).map { PluginIdAndVersion(pluginId, it) }
  }

}