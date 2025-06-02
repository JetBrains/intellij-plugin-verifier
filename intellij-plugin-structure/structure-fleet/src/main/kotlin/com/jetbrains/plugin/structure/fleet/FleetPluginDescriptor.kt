package com.jetbrains.plugin.structure.fleet

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.fleet.problems.InvalidSupportedProductsListProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetPluginDescriptor(
  @JsonProperty(FleetDescriptorSpec.ID_FIELD_NAME)
  val id: String? = null,
  @JsonProperty(FleetDescriptorSpec.VERSION_FIELD_NAME)
  val version: String? = null,
  @JsonProperty(FleetDescriptorSpec.CompatibleShipVersion.NAME)
  val compatibleShipVersionRange: FleetShipVersionRange? = null,
  @JsonProperty(FleetDescriptorSpec.Meta.NAME)
  val meta: FleetMeta? = null,
) {
  companion object {
    private val ID_REGEX = "^[\\w.]+$".toRegex()

    fun parse(serializedDescriptor: String): FleetPluginDescriptor {
      return jacksonObjectMapper().readValue(serializedDescriptor, FleetPluginDescriptor::class.java)
    }
  }

  fun validate(): List<PluginProblem> {
    val problems = mutableListOf<PluginProblem>()
    when {
      id.isNullOrBlank() -> {
        problems.add(PropertyNotSpecified(FleetDescriptorSpec.ID_FIELD_NAME))
      }

      !ID_REGEX.matches(id) -> {
        problems.add(InvalidPluginIDProblem(id))
      }
    }

    if (version.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(FleetDescriptorSpec.VERSION_FIELD_NAME))
    }

    val metaSpec = FleetDescriptorSpec.Meta
    if (meta?.description.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(metaSpec.relativeFieldPath(metaSpec.DESCRIPTION_FIELD_NAME)))
    }

    if (meta?.vendor.isNullOrBlank()) {
      problems.add(PropertyNotSpecified(metaSpec.relativeFieldPath(metaSpec.VENDOR_FIELD_NAME)))
    }

    if (id != SHIP_PLUGIN_ID) {
      when {
        compatibleShipVersionRange == null -> {
          problems.add(PropertyNotSpecified(shipVersionSpec.NAME))
        }

        compatibleShipVersionRange.from.isNullOrBlank() -> {
          problems.add(PropertyNotSpecified(shipVersionSpec.relativeFieldPath(shipVersionSpec.FROM_FIELD_NAME)))
        }

        compatibleShipVersionRange.to.isNullOrBlank() -> {
          problems.add(PropertyNotSpecified(shipVersionSpec.relativeFieldPath(shipVersionSpec.TO_FIELD_NAME)))
        }

        else -> {
          val fromSemver = parseVersionOrNull(compatibleShipVersionRange.from)
          val toSemver = parseVersionOrNull(compatibleShipVersionRange.to)
          when {
            fromSemver == null -> {
              problems.add(InvalidSemverFormat(
                descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
                versionName = shipVersionSpec.relativeFieldPath(shipVersionSpec.FROM_FIELD_NAME),
                version = compatibleShipVersionRange.from
              ))
            }

            toSemver == null -> {
              problems.add(InvalidSemverFormat(
                descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
                versionName = shipVersionSpec.relativeFieldPath(shipVersionSpec.TO_FIELD_NAME),
                version = compatibleShipVersionRange.to
              ))
            }

            fromSemver.isGreaterThan(toSemver) -> {
              problems.add(InvalidVersionRange(
                descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
                since = compatibleShipVersionRange.from,
                until = compatibleShipVersionRange.to
              ))
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
        problems.add(PropertyNotSpecified(metaSpec.relativeFieldPath(metaSpec.NAME_FIELD_NAME)))
      }

      else -> {
        validatePropertyLength(
          descriptor = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
          propertyName = metaSpec.relativeFieldPath(metaSpec.NAME_FIELD_NAME),
          propertyValue = readableName,
          maxLength = MAX_NAME_LENGTH,
          problems = problems
        )
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
      semver.major > FleetShipVersionRange.VERSION_MAJOR_PART_MAX_VALUE -> problems.add(SemverComponentLimitExceeded(
        descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
        componentName = "major",
        versionName = "compatibleShipVersionRange.$versionName",
        version = semver.originalValue,
        limit = FleetShipVersionRange.VERSION_MAJOR_PART_MAX_VALUE
      ))
      semver.minor > FleetShipVersionRange.VERSION_MINOR_PART_MAX_VALUE -> problems.add(SemverComponentLimitExceeded(
        descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
        componentName = "minor",
        versionName = "compatibleShipVersionRange.$versionName",
        version = semver.originalValue,
        limit = FleetShipVersionRange.VERSION_MINOR_PART_MAX_VALUE
      ))
      semver.patch > FleetShipVersionRange.VERSION_PATCH_PART_MAX_VALUE -> problems.add(SemverComponentLimitExceeded(
        descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
        componentName = "patch",
        versionName = "compatibleShipVersionRange.$versionName",
        version = semver.originalValue,
        limit = FleetShipVersionRange.VERSION_PATCH_PART_MAX_VALUE
      ))
    }
    return problems
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetMeta(
  @JsonProperty(FleetDescriptorSpec.Meta.NAME_FIELD_NAME)
  val name: String? = null,
  @JsonProperty(FleetDescriptorSpec.Meta.DESCRIPTION_FIELD_NAME)
  val description: String? = null,
  @JsonProperty(FleetDescriptorSpec.Meta.VENDOR_FIELD_NAME)
  val vendor: String? = null,
  @JsonProperty(FleetDescriptorSpec.Meta.FRONTEND_ONLY_FIELD_NAME)
  val frontendOnly: Boolean? = null,
  @JsonProperty(FleetDescriptorSpec.Meta.HUMAN_VISIBLE_FIELD_NAME)
  val humanVisible: Boolean?,
  @JsonProperty(FleetDescriptorSpec.Meta.SUPPORTED_PRODUCTS_FIELD_NAME)
  val supportedProducts: Set<String>? = emptySet(),
)

data class FleetShipVersionRange(
  @JsonProperty(FleetDescriptorSpec.CompatibleShipVersion.FROM_FIELD_NAME)
  val from: String? = null,
  @JsonProperty(FleetDescriptorSpec.CompatibleShipVersion.TO_FIELD_NAME)
  val to: String? = null
) {

  companion object {
    private const val VERSION_PATCH_LENGTH = 14
    private const val VERSION_MINOR_LENGTH = 13

    const val VERSION_MAJOR_PART_MAX_VALUE = 7449 // 1110100011001
    const val VERSION_MINOR_PART_MAX_VALUE = 1.shl(VERSION_MINOR_LENGTH) - 1 // 8191
    const val VERSION_PATCH_PART_MAX_VALUE = 1.shl(VERSION_PATCH_LENGTH) - 1 // 16383

    // For binary backward compatibility
    fun fromStringToLong(version: String?): Long = fromStringToLong(version, setOf(FleetProduct.FL.productCode))

    fun fromStringToLong(version: String?, supportedProducts: Set<String>): Long {
      require(supportedProducts.isNotEmpty()) { "supportedProducts must not be empty" }
      val products = supportedProducts.mapNotNull { FleetProduct.fromProductCode(it) }.toSet()
      require(products.size == supportedProducts.size) {
        "supportedProducts must contain only product codes from ${FleetProduct.values().map { it.productCode }}, got: $supportedProducts"
      }
      val (legacyVersioning, unifiedVersioning) = products.partition { it.legacyVersioning }
      require(legacyVersioning.isEmpty() || unifiedVersioning.isEmpty()) {
        "supportedProducts must contain either only legacy or only unified versioning products"
      }

      return when {
        version == null -> error("version must not be null")
        legacyVersioning.isNotEmpty() -> resolveLegacyVersion(version) // legacy FL versioning number
        unifiedVersioning.isNotEmpty() -> IdeVersion.createIdeVersion(version).asLong() // unified version format for IntelliJ Products https://youtrack.jetbrains.com/articles/IJPL-A-109
        else -> error("impossible case")
      }
    }

    private fun resolveLegacyVersion(version: String): Long {
      val v = Semver(version)
      return v.major.toLong().shl(VERSION_PATCH_LENGTH + VERSION_MINOR_LENGTH) + v.minor.toLong().shl(VERSION_PATCH_LENGTH) + v.patch
    }
  }
}

enum class FleetProduct(
  val productCode: String,
  val legacyVersioning: Boolean = false,
) {
  FL("FL", legacyVersioning = true),
  AIR("AIR"),
  AIR_NEXT("AINEXT"); // TODO: product code is susceptible to change

  companion object {
    fun fromProductCode(productCode: String): FleetProduct? = values().toList().find { it.productCode == productCode }
  }
}
