package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.edu.problems.InvalidVersionError


data class EduPluginVersion(
  val pluginVersion: String,
  val ideVersion: String,
  val buildNumber: String
) {
  companion object {
    fun fromString(version: String?): EduPluginVersion {
      if (version.isNullOrEmpty()) fail(version)
      val components = version.split("-")
      if (components.size != 3) fail(version)
      return EduPluginVersion(components[0], components[1], components[2])
    }

    fun createIfValid(version: String?, problems: MutableList<PluginProblem>): EduPluginVersion? {
      if (version.isNullOrBlank()) {
        problems.add(PropertyNotSpecified(VERSION))
        return null
      }

      return try {
        fromString(version)
      } catch (e: IllegalArgumentException) {
        problems.add(InvalidVersionError(version))
        null
      }
    }

    private fun fail(version: String?): Nothing {
      throw IllegalArgumentException("$version doesn't represent edu plugin version")
    }
  }
}