package com.jetbrains.plugin.structure.edu.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.edu.EDU_PLUGIN_VERSION
import com.jetbrains.plugin.structure.edu.PROGRAMMING_LANGUAGE
import com.jetbrains.plugin.structure.edu.bean.EduVendor

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EduItem(
  var title: String? = ""
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EduPluginJsonBuilder(
  var title: String? = "key",
  var summary: String? = "summary",
  var language: String? = "en",
  var vendor: EduVendor? = EduVendor("JetBrains s.r.o.", "", "http://jetbrains.com/"),
  @JsonProperty(PROGRAMMING_LANGUAGE)
  var programmingLanguage: String? = "kotlin",
  var items: List<EduItem>? = listOf(EduItem("lesson1"), EduItem("lesson2")),
  @JsonProperty(EDU_PLUGIN_VERSION)
  var version: String? = "3.7-2019.3-5266"
) {

  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

val perfectEduPluginBuilder: EduPluginJsonBuilder
  get() = EduPluginJsonBuilder()


fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/edu/$fileName.json").readText()
}
