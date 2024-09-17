package com.jetbrains.plugin.structure.intellij.problems.remapping

enum class RemappingSet(val id: String) {
  JETBRAINS_PLUGIN_REMAPPING_SET("jetbrains-plugin"),
  EXISTING_PLUGIN_REMAPPING_SET("existing-plugin"),
  NEW_PLUGIN_REMAPPING_SET("new-plugin"),
  CLI_IGNORED("cli-ignored");

  companion object {
    fun fromId(id: String): RemappingSet {
      return values().firstOrNull { it.id == id }
        ?: throw IllegalArgumentException("Unknown remapping set id: $id")
    }
  }
}