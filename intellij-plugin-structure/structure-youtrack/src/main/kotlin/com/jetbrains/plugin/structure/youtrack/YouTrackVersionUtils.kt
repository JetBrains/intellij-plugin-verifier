package com.jetbrains.plugin.structure.youtrack

import com.vdurmont.semver4j.Semver

/*
  YouTrack version is in semver format, where:
    - `MAJOR` is the year,
    - `MINOR` is the release within that year,
    - `PATCH` is a build number.

  For instance, `2024.3.35000`.
 */
object YouTrackVersionUtils {
  const val MAX_MAJOR_VALUE = 3000
  const val VERSION_MINOR_LENGTH = 100
  const val VERSION_PATCH_LENGTH = 1000000

  fun versionAsLong(youTrackVersion: String?): Long? {
    if (youTrackVersion == null) return null

    val semanticVersion = getSemverFromString(youTrackVersion)

    return semanticVersion.run {
      major.toLong() * (VERSION_PATCH_LENGTH * VERSION_MINOR_LENGTH) + minor.toLong() * VERSION_PATCH_LENGTH + patch
    }
  }

  fun getSemverFromString(youTrackVersion: String): Semver {
    return Semver(youTrackVersion, Semver.SemverType.STRICT)
  }
}