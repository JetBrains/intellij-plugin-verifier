package com.jetbrains.plugin.structure.teamcity.recipe

import com.vdurmont.semver4j.Semver

object TeamCityRecipeSpecVersionUtils {
  const val MAX_MAJOR_VALUE = 10000
  const val VERSION_MINOR_LENGTH = 10000
  const val VERSION_PATCH_LENGTH = 10000

  fun versionAsLong(specVersion: String?): Long? {
    if (specVersion == null) return null

    val semanticVersion = getSemverFromString(specVersion)

    return semanticVersion.run {
      major.toLong() * (VERSION_PATCH_LENGTH * VERSION_MINOR_LENGTH) + minor.toLong() * VERSION_PATCH_LENGTH + patch
    }
  }

  fun getSemverFromString(version: String): Semver {
    return Semver(version, Semver.SemverType.LOOSE)
  }
}