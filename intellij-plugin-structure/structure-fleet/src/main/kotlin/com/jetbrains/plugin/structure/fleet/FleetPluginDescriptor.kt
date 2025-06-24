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

    val supportedProducts = meta?.getSupportedProductCodes() ?: emptySet()
    val products = supportedProducts.mapNotNull { FleetProduct.fromProductCode(it) }.toSet()

    if (products.size != supportedProducts.size) {
      problems.add(InvalidSupportedProductsListProblem(
        constraint = "must contain only product codes from ${FleetProduct.values().map { it.productCode }}, got: $supportedProducts"
      ))
    }
    val (isLegacyVersioning, isUnifiedVersioning) = products.partition { it.legacyVersioning }.let { (legacy, unified) ->
      Pair(legacy.isNotEmpty(), unified.isNotEmpty())
    }
    if (isLegacyVersioning && isUnifiedVersioning) {
      problems.add(InvalidSupportedProductsListProblem(
        constraint = "must contain either only legacy or only unified versioning products"
      ))
    }

    val shipVersionSpec = FleetDescriptorSpec.CompatibleShipVersion
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
              val fromVersionProblems = validateVersion(
                fieldName = shipVersionSpec.relativeFieldPath(shipVersionSpec.FROM_FIELD_NAME),
                semver = fromSemver,
                isLegacy = isLegacyVersioning
              )
              problems.addAll(fromVersionProblems)
              if (fromVersionProblems.isEmpty()) {
                problems.addAll(validateVersion(
                  fieldName = shipVersionSpec.relativeFieldPath(shipVersionSpec.TO_FIELD_NAME),
                  semver = toSemver,
                  isLegacy = isLegacyVersioning
                ))
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
    } catch (_: SemverException) {
      null
    }
  }

  private fun validateVersion(fieldName: String, semver: Semver, isLegacy: Boolean): Collection<PluginProblem> {
    val problems = mutableListOf<PluginProblem>()
    val (majorPartMaxValue, minorPartMaxValue, patchPartMaxValue) = FleetDescriptorSpec.CompatibleShipVersion.getVersionConstraints(isLegacy)

    when {
      semver.major > majorPartMaxValue -> problems.add(SemverComponentLimitExceeded(
        descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
        componentName = "major",
        versionName = fieldName,
        version = semver.originalValue,
        limit = majorPartMaxValue
      ))
      semver.minor > minorPartMaxValue -> problems.add(SemverComponentLimitExceeded(
        descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
        componentName = "minor",
        versionName = fieldName,
        version = semver.originalValue,
        limit = minorPartMaxValue
      ))
      semver.patch > patchPartMaxValue -> problems.add(SemverComponentLimitExceeded(
        descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
        componentName = "patch",
        versionName = fieldName,
        version = semver.originalValue,
        limit = patchPartMaxValue
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
  private val supportedProducts: String? = null
) {
  fun getSupportedProductCodes(): Set<String> {
    if (supportedProducts.isNullOrBlank()) return emptySet()
    return supportedProducts
      .split(",")
      .toSet()
  }
}

data class FleetShipVersionRange(
  @JsonProperty(FleetDescriptorSpec.CompatibleShipVersion.FROM_FIELD_NAME)
  val from: String? = null,
  @JsonProperty(FleetDescriptorSpec.CompatibleShipVersion.TO_FIELD_NAME)
  val to: String? = null
) {

  companion object {
    // For binary backward compatibility
    fun fromStringToLong(version: String): Long = fromStringToLong(version, setOf(FleetProduct.FL.productCode))

    fun fromStringToLong(version: String, supportedProducts: Set<String>): Long {
      val products = supportedProducts.mapNotNull { FleetProduct.fromProductCode(it) }.toSet()

      return when {
        products.any { it.legacyVersioning.not() } -> IdeVersion.createIdeVersion(version).asLong() // unified version format for IntelliJ Products https://youtrack.jetbrains.com/articles/IJPL-A-109
        else -> resolveLegacyVersion(version) // legacy FL versioning number
      }
    }

    private fun resolveLegacyVersion(version: String): Long {
      val v = Semver(version)
      val versionSpec = FleetDescriptorSpec.CompatibleShipVersion.LegacyVersioningSpec
      return v.major.toLong().shl(versionSpec.VERSION_PATCH_LENGTH + versionSpec.VERSION_MINOR_LENGTH) +
        v.minor.toLong().shl(versionSpec.VERSION_PATCH_LENGTH) + v.patch
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
