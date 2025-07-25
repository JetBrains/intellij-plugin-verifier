package com.jetbrains.plugin.structure.toolbox

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.*
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException
import java.net.MalformedURLException
import java.net.URL

@JsonIgnoreProperties(ignoreUnknown = true)
data class ToolboxPluginDescriptor(
  @JsonProperty("id")
  val id: String? = null,
  @JsonProperty("version")
  val version: String? = null,
  @JsonProperty("apiVersion")
  val apiVersion: String? = null,
  @JsonProperty("meta")
  val meta: ToolboxMeta? = null,
) {
  companion object {
    private val ID_REGEX = "^[\\w.]+$".toRegex()

    fun parse(serializedDescriptor: String): ToolboxPluginDescriptor {
      return jacksonObjectMapper().readValue(serializedDescriptor, ToolboxPluginDescriptor::class.java)
    }
  }

  fun validate(): List<PluginProblem> {
    val problems = mutableListOf<PluginProblem>()
    when {
      id.isNullOrBlank() -> {
        problems.add(PropertyNotSpecified("id"))
      }

      !ID_REGEX.matches(id) -> {
        problems.add(InvalidPluginIDProblem(id))
      }
    }

    if (version.isNullOrBlank()) {
      problems.add(PropertyNotSpecified("version"))
    }

    if (meta?.description.isNullOrBlank()) {
      problems.add(PropertyNotSpecified("meta.description"))
    }

    if (meta?.vendor.isNullOrBlank()) {
      problems.add(PropertyNotSpecified("meta.vendor"))
    }

    if (apiVersion.isNullOrBlank()) {
      problems.add(PropertyNotSpecified("apiVersion"))
    } else {
      val apiVersionParsed = parseVersionOrNull(apiVersion)
      if (apiVersionParsed == null) {
        problems.add(
          InvalidSemverFormat(
            descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
            versionName = "apiVersion",
            version = apiVersion
          )
        )
      } else {
        problems.addAll(validateVersion("apiVersion", apiVersionParsed))
      }
    }

    val readableName = meta?.name
    when {
      readableName.isNullOrBlank() -> {
        problems.add(PropertyNotSpecified("meta.name"))
      }

      else -> {
        validatePropertyLength(
          descriptor = ToolboxPluginManager.DESCRIPTOR_NAME,
          propertyName = "meta.name",
          propertyValue = readableName,
          maxLength = MAX_NAME_LENGTH,
          problems = problems
        )
        validatePluginNameIsCorrect(
          descriptor = ToolboxPluginManager.DESCRIPTOR_NAME,
          name = readableName,
          problems = problems
        )
      }
    }
    val metaUrl = meta?.url
    if (metaUrl != null) {
      try {
        URL(metaUrl)
      } catch (_: MalformedURLException) {
        problems.add(InvalidUrl(metaUrl, "meta.url"))
      }
    }
    return problems
  }

  private fun parseVersionOrNull(version: String): Semver? {
    return try {
      Semver(version)
    } catch (_: SemverException) {
      null
    }
  }

  @Suppress("SameParameterValue")
  private fun validateVersion(versionName: String, semver: Semver): Collection<PluginProblem> {
    val problems = mutableListOf<PluginProblem>()
    when {
      semver.major > ToolboxVersionRange.VERSION_MAJOR_PART_MAX_VALUE -> problems.add(
        SemverComponentLimitExceeded(
          descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
          componentName = "major",
          versionName = versionName,
          version = semver.originalValue,
          limit = ToolboxVersionRange.VERSION_MAJOR_PART_MAX_VALUE
        )
      )
      semver.minor > ToolboxVersionRange.VERSION_MINOR_PART_MAX_VALUE -> problems.add(
        SemverComponentLimitExceeded(
          descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
          componentName = "minor",
          versionName = versionName,
          version = semver.originalValue,
          limit = ToolboxVersionRange.VERSION_MINOR_PART_MAX_VALUE
        )
      )
      semver.patch > ToolboxVersionRange.VERSION_PATCH_PART_MAX_VALUE -> problems.add(
        SemverComponentLimitExceeded(
          descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
          componentName = "patch",
          versionName = versionName,
          version = semver.originalValue,
          limit = ToolboxVersionRange.VERSION_PATCH_PART_MAX_VALUE
        )
      )
    }
    return problems
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ToolboxMeta(
  @JsonProperty("readableName")
  val name: String? = null,
  @JsonProperty("description")
  val description: String? = null,
  @JsonProperty("vendor")
  val vendor: String? = null,
  @JsonProperty("url")
  val url: String? = null,
)

data class ToolboxVersionRange(
  @JsonProperty("from")
  val from: String? = null,
  @JsonProperty("to")
  val to: String? = null
) {
  companion object {
    private const val VERSION_PATCH_LENGTH = 20
    private const val VERSION_MINOR_LENGTH = 13

    const val VERSION_MAJOR_PART_MAX_VALUE = 7449 // 1110100011001
    const val VERSION_MINOR_PART_MAX_VALUE = 1.shl(VERSION_MINOR_LENGTH) - 1 // 8191
    const val VERSION_PATCH_PART_MAX_VALUE = 1.shl(VERSION_PATCH_LENGTH) - 1 // 1048575

    fun fromStringToLong(version: String?): Long {
      return Semver(version).run {
        major.toLong().shl(VERSION_PATCH_LENGTH + VERSION_MINOR_LENGTH) + minor.toLong().shl(VERSION_PATCH_LENGTH) + patch
      }
    }
  }

  @Suppress("unused")
  fun asLongRange(): LongRange {
    val fromLong = fromStringToLong(from)
    val toLong = fromStringToLong(to)
    return fromLong..toLong
  }
}