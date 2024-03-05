package com.jetbrains.plugin.structure.fleet

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.*
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetPluginDescriptor(
  @JsonProperty("id")
  val id: String? = null,
  @JsonProperty("version")
  val version: String? = null,
  @JsonProperty("compatibleShipVersionRange")
  val compatibleShipVersionRange: FleetShipVersionRange? = null,
  @JsonProperty("meta")
  val meta: FleetMeta? = null,
) {
  companion object {
    private val NON_ID_SYMBOL_REGEX = "^[A-Za-z\\d_.]+$".toRegex()

    fun parse(serializedDescriptor: String): FleetPluginDescriptor {
      return jacksonObjectMapper().readValue(serializedDescriptor, FleetPluginDescriptor::class.java)
    }
  }

  fun validate(): List<PluginProblem> {
    val problems = mutableListOf<PluginProblem>()
    when {
      id.isNullOrBlank() -> {
        problems.add(PropertyNotSpecified("id"))
      }

      !NON_ID_SYMBOL_REGEX.matches(id) -> {
        problems.add(InvalidPluginIDProblem(id))
      }
    }

    if (version.isNullOrBlank()) {
      problems.add(PropertyNotSpecified("version"))
    }

    if (meta?.description.isNullOrBlank()) {
      problems.add(PropertyNotSpecified("description"))
    }

    if (meta?.vendor.isNullOrBlank()) {
      problems.add(PropertyNotSpecified("vendor"))
    }

    if (id != SHIP_PLUGIN_ID) {
      when {
        compatibleShipVersionRange == null -> {
          problems.add(PropertyNotSpecified("compatibleShipVersionRange"))
        }

        compatibleShipVersionRange.from.isNullOrBlank() -> {
          problems.add(PropertyNotSpecified("compatibleShipVersionRange.from"))
        }

        compatibleShipVersionRange.to.isNullOrBlank() -> {
          problems.add(PropertyNotSpecified("compatibleShipVersionRange.to"))
        }

        else -> {
          val fromSemver = parseVersionOrNull(compatibleShipVersionRange.from)
          val toSemver = parseVersionOrNull(compatibleShipVersionRange.to)
          when {
            fromSemver == null -> {
              problems.add(FleetInvalidShipVersion("from", compatibleShipVersionRange.from))
            }

            toSemver == null -> {
              problems.add(FleetInvalidShipVersion("to", compatibleShipVersionRange.to))
            }

            fromSemver.isGreaterThan(toSemver) -> {
              problems.add(FleetInvalidShipVersionRange(compatibleShipVersionRange.from, compatibleShipVersionRange.to))
            }

            else -> {
              val fromVersionProblems = validateVersion("from", fromSemver)
              problems.addAll(fromVersionProblems)
              if (fromVersionProblems.isEmpty()) {
                problems.addAll(validateVersion("to", toSemver))
              }
            }
          }
        }
      }
    }

    val readableName = meta?.name
    when {
      readableName.isNullOrBlank() -> {
        problems.add(PropertyNotSpecified("name"))
      }

      else -> {
        validatePropertyLength(FleetPluginManager.DESCRIPTOR_NAME, "name", readableName, MAX_NAME_LENGTH, problems)
      }
    }
    return problems
  }

  private fun parseVersionOrNull(version: String): Semver? {
    return try {
      Semver(version)
    } catch (e: SemverException) {
      null
    }
  }

  private fun validateVersion(versionName: String, semver: Semver): Collection<PluginProblem> {
    val problems = mutableListOf<PluginProblem>()
    when {
      semver.major > VERSION_MAJOR_PART_MAX_VALUE -> problems.add(FleetErroneousShipVersion(versionName, "major", semver.originalValue, VERSION_MAJOR_PART_MAX_VALUE))
      semver.minor > VERSION_MINOR_PART_MAX_VALUE -> problems.add(FleetErroneousShipVersion(versionName, "minor", semver.originalValue, VERSION_MINOR_PART_MAX_VALUE))
      semver.patch > VERSION_PATCH_PART_MAX_VALUE -> problems.add(FleetErroneousShipVersion(versionName, "patch", semver.originalValue, VERSION_PATCH_PART_MAX_VALUE))
    }
    return problems
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetMeta(
  @JsonProperty("readableName")
  val name: String? = null,
  @JsonProperty("description")
  val description: String? = null,
  @JsonProperty("vendor")
  val vendor: String? = null,
  @JsonProperty("frontend-only")
  val frontendOnly: Boolean? = null
)

data class FleetShipVersionRange(
  @JsonProperty("from")
  val from: String? = null,
  @JsonProperty("to")
  val to: String? = null
) {

  companion object {
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

private const val VERSION_PATCH_LENGTH = 14
private const val VERSION_MINOR_LENGTH = 13

const val VERSION_MAJOR_PART_MAX_VALUE = 7449 // 1110100011001
const val VERSION_MINOR_PART_MAX_VALUE = 1.shl(VERSION_MINOR_LENGTH) - 1 // 8191
const val VERSION_PATCH_PART_MAX_VALUE = 1.shl(VERSION_PATCH_LENGTH) - 1 // 16383

class FleetInvalidShipVersion(
  versionName: String,
  version: String
) : InvalidDescriptorProblem(
  descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
  detailedMessage = "The `compatibleShipVersionRange.$versionName` version should be formatted as semver [$version]."
) {
  override val level
    get() = Level.ERROR
}

class FleetInvalidShipVersionRange(from: String, to: String) : InvalidDescriptorProblem(
  descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
  detailedMessage = "The `compatibleShipVersionRange.from` build $from is greater than `compatibleShipVersionRange.to` build $to."
) {
  override val level
    get() = Level.ERROR
}

class FleetErroneousShipVersion(
  versionName: String,
  partName: String,
  version: String,
  limit: Int
) : InvalidDescriptorProblem(
  descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
  detailedMessage = "The $partName part of `compatibleShipVersionRange.$versionName` version is too big [$version]. Max value is $limit."
) {
  override val level: Level
    get() = Level.ERROR
}
