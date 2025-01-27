package com.jetbrains.plugin.structure.hub.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class HubPluginJsonBuilder(
    var key: String? = "key",
    var name: String? = "name",
    var version: String? = "version",
    var description: String? = "description",
    var author: String? = "A B a@gmail.com",
    var homeUrl: String? = "www.google.com",
    var iconUrl: String? = null,
    var dependencies: Map<String, String>? = mapOf("a" to "1.0"),
    var products: Map<String, String>? = mapOf("a" to "1.0"),
    var view: Map<String, String>? = emptyMap()
) {
  private val objectMapper = jacksonObjectMapper()

  fun asString(): String {
    val nonEmptyKeyValues = mapOf(
      "key" to key,
      "name" to name,
      "version" to version,
      "description" to description,
      "author" to author,
      "homeUrl" to homeUrl,
      "iconUrl" to iconUrl,
      "dependencies" to dependencies,
      "products" to products,
      "view" to view
    ).filterValues { value -> value != null }
    return objectMapper.writeValueAsString(nonEmptyKeyValues)
  }
}

val perfectHubPluginBuilder: HubPluginJsonBuilder
  get() = HubPluginJsonBuilder()

fun HubPluginJsonBuilder.modify(block: HubPluginJsonBuilder.() -> Unit): String {
  val copy = copy()
  copy.block()
  return copy.asString()
}