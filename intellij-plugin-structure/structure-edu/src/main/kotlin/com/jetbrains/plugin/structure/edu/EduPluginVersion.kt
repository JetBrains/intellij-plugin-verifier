package com.jetbrains.plugin.structure.edu

data class EduPluginVersion(val pluginVersion: String, val ideVersion: String, val buildNumber: String) {
  companion object {
    fun parse(version: String?): EduPluginVersion {
      if (version == null) fail(version)
      val components = version.split("-")
      if (components.size != 3) fail(version)
      return EduPluginVersion(components[0], components[1], components[2])
    }

    private fun fail(version: String?): Nothing {
      throw IllegalArgumentException("$version doesn't represent edu plugin version")
    }
  }
}
