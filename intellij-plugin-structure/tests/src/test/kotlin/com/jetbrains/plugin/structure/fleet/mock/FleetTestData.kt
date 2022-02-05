package com.jetbrains.plugin.structure.fleet.mock

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.fleet.bean.DependencyVersion
import com.jetbrains.plugin.structure.fleet.bean.Part


@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetTestDescriptor(
  var id: String? = "fleet.language.css",
  var name: String? = "CSS",
  var version: String? = "1.0.0-SNAPSHOT",
  var description: String? = "CSS language support",
  var vendor: String? = "JetBrains",
  val requires: Map<String, DependencyVersion> = mapOf(),
  val frontend: Part? = Part(listOf("f-1.1.1.jar"), listOf("f-cp.jar"), listOf("f")),
  val workspace: Part? = null,
) {
  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

val perfectFleetPluginBuilder
  get() = FleetTestDescriptor()

fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/fleet/$fileName.json").readText()
}
