package com.jetbrains.plugin.structure.dotnet

//Copy of NuGet.SemVer from C#. Used for version normalization and same parsing as in NuGet gallery
data class NugetSemanticVerison(
    val majorVersion: Int, val minorVersion: Int, val build: Int = 0, val revision: Int = 0, val release: String? = null, val metadata: String? = null
) {
  companion object {
    private val semanticVersionRegex = Regex("^(?<Version>\\d+(\\s*\\.\\s*\\d+){0,3})(?<Release>-([0]\\b|[0]$|[0][0-9]*[A-Za-z-]+|[1-9A-Za-z-][0-9A-Za-z-]*)+(\\.([0]\\b|[0]$|[0][0-9]*[A-Za-z-]+|[1-9A-Za-z-][0-9A-Za-z-]*)+)*)?(?<Metadata>\\+[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?$")

    fun parse(version: String): NugetSemanticVerison {
      val match = semanticVersionRegex.matchEntire(version.trim()) ?: fail(version)
      val verison = parseVersionPart(match.groups["Version"]?.value)
      val release = match.groups["Release"]?.value?.substring(1)
      val metadata = match.groups["Metadata"]?.value?.substring(1)
      return verison?.copy(release = release, metadata = metadata) ?: fail(version)
    }

    private fun parseVersionPart(versionString: String?): NugetSemanticVerison? {
      if (versionString == null) return null
      val components = versionString.split('.')
      if (components.size < 2 || components.size > 4) return null
      return NugetSemanticVerison(
          components[0].toInt(),
          components[1].toInt(),
          components.getOrNull(2)?.toInt() ?: 0,
          components.getOrNull(3)?.toInt() ?: 0
      )
    }

    private fun fail(version: String): Nothing {
      throw IllegalArgumentException("$version doesn't represent NuGet package version")
    }
  }

  val normalizedVersionString = buildString {
    append(majorVersion).append('.').append(minorVersion).append('.').append(Math.max(0, build))
    if (revision > 0) {
      append('.').append(revision)
    }
    if (!release.isNullOrBlank()) {
      append('-').append(release)
    }
  }
}
