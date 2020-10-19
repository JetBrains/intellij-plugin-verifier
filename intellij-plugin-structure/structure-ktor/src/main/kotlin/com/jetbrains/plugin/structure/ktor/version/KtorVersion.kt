package com.jetbrains.plugin.structure.ktor.version

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.ktor.bean.KTOR_VERSION
import com.jetbrains.plugin.structure.ktor.problems.IncorrectKtorVersionFormat


// Ktor version is represented in a format of x.y.z-*
data class KtorVersion(val x: Int, val y: Int, val z: Int): Comparable<KtorVersion> {
  companion object {
    fun fromString(version: String): KtorVersion {
      if (version.isBlank()) {
        throw IllegalArgumentException("Ktor version string must not be empty")
      }

      val parts = version
        .split("-")
        .first()
        .split(".")

      if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) {
        throw IllegalArgumentException("Invalid version number: $version")
      }

      return KtorVersion(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    fun createIfValid(version: String?, problems: MutableList<PluginProblem>): KtorVersion? {
      if (version.isNullOrBlank()) {
        problems.add(PropertyNotSpecified(KTOR_VERSION))
        return null
      }

      return try {
        fromString(version)
      } catch (e: IllegalArgumentException) {
        problems.add(IncorrectKtorVersionFormat(version))
        null
      }
    }
  }

  fun asString(): String = "$x.$y.$z"

  override fun compareTo(other: KtorVersion): Int = when {
    x < other.x -> -1
    x > other.x -> 1
    y < other.y -> -1
    y > other.y -> 1
    z < other.z -> -1
    z > other.z -> 1
    else -> 0
  }
}