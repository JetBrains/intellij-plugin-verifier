package com.jetbrains.plugin.structure.fleet

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidPluginIDProblem
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.validatePropertyLength

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetPluginDescriptor(
  @JsonProperty("id")
  val id: String? = null,
  @JsonProperty("version")
  val version: String? = null,
  @JsonProperty("compatibleShipVersionRange")
  val compatibleShipVersionRange: ShipVersionRange? = null,
  @JsonProperty("meta")
  val meta: FleetMeta? = null
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
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetMeta(
  @JsonProperty("readableName")
  val name: String? = null,
  @JsonProperty("description")
  val description: String? = null,
  @JsonProperty("vendor")
  val vendor: String? = null
)

data class ShipVersionRange(
  @JsonProperty("from")
  val from: String? = null,
  @JsonProperty("to")
  val to: String? = null
)