package com.jetbrains.plugin.structure.edu.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.edu.*


data class EduPluginJsonBuilder(
  var title: String? = "key",
  var summary: String? = "summary",
  var language: String? = "en",
  var vendor: String? = "JetBrains s.r.o.",
  var programmingLanguage: String? = "kotlin",
  var items: List<String>? = listOf("lesson1", "lesson2"),
  var version: String? = "3.7-2019.3-5266"
) {

  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

val perfectEduPluginBuilder: EduPluginJsonBuilder
  get() = EduPluginJsonBuilder()


fun getMockPluginJsonContent(fileName: String): String {
  return object{}.javaClass.getResource("/edu/$fileName.json").readText()
}
