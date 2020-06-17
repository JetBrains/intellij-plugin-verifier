package com.jetbrains.plugin.structure.edu.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


@JsonInclude(JsonInclude.Include.NON_NULL)
data class EduPluginJsonBuilder(
  var summary: String? = "summary",
  var language: String? = "language",
  var title: String? = "title",
  var version: String? = "version",
  var programming_language: String? = "programming_language"
) {

  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

val perfectEduPluginBuilder: EduPluginJsonBuilder
  get() = EduPluginJsonBuilder()

fun EduPluginJsonBuilder.modify(block: EduPluginJsonBuilder.() -> Unit): String {
  val copy = copy()
  copy.block()
  return copy.asString()
}

fun getMockPluginJsonContent(fileName: String): String {
  return object{}.javaClass.getResource("/edu/$fileName.json").readText()
}
