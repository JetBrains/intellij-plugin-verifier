package com.jetbrains.plugin.structure.ktor.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.ktor.bean.KtorVendor

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KtorPluginJsonBuilder(
  var id: String? = "feature.ktor",
  var name: String? = "Ktor feature",
  var version: String? = "1.0.0",
  var description: String? = "description",
  var copyright: String? = "copyright",
  var vendor: KtorVendor? = KtorVendor("JetBrains s.r.o.", "", "http://jetbrains.com/")
) {

  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

val perfectEduPluginBuilder: KtorPluginJsonBuilder
  get() = KtorPluginJsonBuilder()


fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/ktor/$fileName.json").readText()
}
