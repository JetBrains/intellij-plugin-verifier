package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.dotnet.NugetSemanticVersion
import org.junit.Assert
import org.junit.Test

class NugetSemanticVersionTest {
  @Test
  fun invalidSemverTest() {
    listOf(
      "1",
      "1beta",
      "1.2Av^c",
      "1.2..",
      "1.2.3.4.5",
      "1.2.3.Beta",
      "1.2.3.4This version is full of awesomeness!!",
      "So.is.this",
      "1.34.2Alpha",
      "1.34.2Release Candidate",
      "1.4.7-"
    ).forEach { invalidVersion ->
      try {
        NugetSemanticVersion.parse(invalidVersion)
        Assert.fail("Version $invalidVersion was parsed")
      } catch (e: IllegalArgumentException) {
      }
    }
  }

  @Test
  fun parseLegacyVersions() = mapOf(
    "1.022" to NugetSemanticVersion(1, 22, 0, 0),
    "23.2.3" to NugetSemanticVersion(23, 2, 3, 0),
    "1.3.42.10133" to NugetSemanticVersion(1, 3, 42, 10133)
  ).forEach(assertVersionParsed)

  @Test
  fun parseReadsSemverAndHybridSemverVersionNumbers() = mapOf(
    "1.022-Beta" to NugetSemanticVersion(1, 22, 0, 0, "Beta"),
    "23.2.3-Alpha" to NugetSemanticVersion(23, 2, 3, 0, "Alpha"),
    "1.3.42.10133-PreRelease" to NugetSemanticVersion(1, 3, 42, 10133, "PreRelease"),
    "1.3.42.200930-RC-2" to NugetSemanticVersion(1, 3, 42, 200930, "RC-2")
  ).forEach(assertVersionParsed)

  val assertVersionParsed = { version: String, parsedVersion: NugetSemanticVersion ->
    Assert.assertEquals(parsedVersion, NugetSemanticVersion.parse(version))
  }

  @Test
  fun normalizeVersionTest() = mapOf(
    "1.0" to "1.0.0",
    "1.7" to "1.7.0",
    "1.0.0.0" to "1.0.0",
    "1.0.0" to "1.0.0",
    "1.2.3" to "1.2.3",
    "1.2.03" to "1.2.3",
    "1.2.0.4" to "1.2.0.4",
    "1.2.3.4" to "1.2.3.4",
    "1.2-special" to "1.2.0-special",
    "1.2.3-special" to "1.2.3-special",
    "1.2.3.5-special" to "1.2.3.5-special",
    "1.2.0.5-special" to "1.2.0.5-special"
  ).forEach { version, normalizedVersion ->
    Assert.assertEquals(normalizedVersion, NugetSemanticVersion.parse(version).normalizedVersionString)
  }
}