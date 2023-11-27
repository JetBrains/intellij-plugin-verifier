package com.jetbrains.plugin.structure.edu.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.edu.*
import com.jetbrains.plugin.structure.edu.bean.EduVendor
import java.nio.file.Path

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EduItem(
  var title: String? = "",
  @JsonProperty(CUSTOM_NAME)
  var customName: String? = "",
  var items: List<EduItem>? = null,
  var type: String = ""
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EduPluginJsonBuilder(
  var title: String? = "key",
  var summary: String? = "summary",
  var language: String? = "en",
  var vendor: EduVendor? = EduVendor("JetBrains s.r.o.", "", "http://jetbrains.com/"),
  @JsonProperty(PROGRAMMING_LANGUAGE)
  var programmingLanguage: String? = null,
  @JsonProperty(PROGRAMMING_LANGUAGE_ID)
  var programmingLanguageId: String? = "kotlin",
  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION)
  var programmingLanguageVersion: String? = null,
  var items: List<EduItem>? = listOf(EduItem("lesson1"), EduItem("lesson2")),
  @JsonProperty(DESCRIPTOR_VERSION)
  var descriptorVersion: Int? = 1,
  @JsonProperty(VERSION)
  val pluginVersion: String? = "1.1"
) {

  fun asString(): String = jacksonObjectMapper().writeValueAsString(this)
}

val perfectEduPluginBuilder: EduPluginJsonBuilder
  get() = EduPluginJsonBuilder()

fun buildEduPlugin(filePath: Path, descriptor: EduPluginJsonBuilder.() -> Unit): Path {
  val pluginFile = buildZipFile(filePath) {
    file(EduPluginManager.DESCRIPTOR_NAME) {
      val builder = perfectEduPluginBuilder
      builder.descriptor()
      builder.asString()
    }
  }
  return pluginFile
}

fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/edu/$fileName.json").readText()
}
