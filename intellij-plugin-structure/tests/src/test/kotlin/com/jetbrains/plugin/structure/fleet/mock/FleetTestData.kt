package com.jetbrains.plugin.structure.fleet.mock

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.fleet.bean.FleetDependency


@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetTestDescriptor(
  var id: String? = "fleet.language.css",
  var name: String? = "CSS",
  var version: String? = "1.0.0-SNAPSHOT",
  var description: String? = "CSS language support",
  var entryPoint: String? = "fleet.language.css.Css",
  var vendor: String? = "JetBrains",
  val requires: List<FleetDependency>? = null
) {
  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

val perfectFleetPluginBuilder
  get() = FleetTestDescriptor()

fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/fleet/$fileName.json").readText()
}
