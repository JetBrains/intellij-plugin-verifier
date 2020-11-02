package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.CompatibilityUtils
import com.jetbrains.plugin.structure.edu.problems.InvalidVersionError


data class EduFullPluginVersion(
  val pluginVersion: EduPluginVersion,
  val ideVersion: String,
  val buildNumber: String
): Comparable<EduFullPluginVersion> {
  companion object {
    fun fromString(version: String?): EduFullPluginVersion {
      if (version.isNullOrEmpty()) fail(version)
      val parts = version.split("-")
      if (parts.size != 3 || parts.any { it.isEmpty() }) fail(version)
      return EduFullPluginVersion(EduPluginVersion.fromString(parts[0]), parts[1], parts[2])
    }

    fun createIfValid(version: String?, problems: MutableList<PluginProblem>): EduFullPluginVersion? {
      if (version.isNullOrBlank()) {
        problems.add(PropertyNotSpecified(EDU_PLUGIN_VERSION))
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
      throw IllegalArgumentException("$version doesn't represent full edu plugin version")
    }
  }

  fun asLong() = CompatibilityUtils.versionAsLong(pluginVersion.x, pluginVersion.y, pluginVersion.z)

  override fun compareTo(other: EduFullPluginVersion) = pluginVersion.compareTo(other.pluginVersion)
}

data class EduPluginVersion(val x: Int, val y: Int, val z: Int = 0) : Comparable<EduPluginVersion> {
  companion object {
    fun fromString(version: String): EduPluginVersion {
      if (version.isEmpty()) throw IllegalArgumentException("Edu plugin version shouldn't be empty")

      val components = version.split(".")
      if (components.size < 2 || components.any { it.toIntOrNull() == null }) {
        throw IllegalArgumentException("Edu plugin version is invalid: $version")
      }

      return EduPluginVersion(
        components[0].toInt(),
        components[1].toInt(),
        components.getOrNull(2)?.toInt() ?: 0
      )
    }
  }
  override fun compareTo(other: EduPluginVersion) = when {
    x < other.x -> -1
    x > other.x -> 1
    y < other.y -> -1
    y > other.y -> 1
    z < other.z -> -1
    z > other.z -> 1
    else -> 0
  }
}