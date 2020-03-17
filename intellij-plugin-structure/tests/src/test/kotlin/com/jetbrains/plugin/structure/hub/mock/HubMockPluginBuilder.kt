package com.jetbrains.plugin.structure.hub.mock

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

  private fun Map<*, *>.asJson(indent: String = ""): String =
    entries.joinToString(prefix = "$indent{\n", postfix = "\n$indent}", separator = ",\n") { (key, value) ->
      "$indent  " + when (value) {
        is String -> """"$key": "$value""""
        is Map<*, *> -> """"$key": ${value.asJson("$indent  ")}"""
        else -> ""
      }
    }

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
    return nonEmptyKeyValues.asJson()
  }
}

val perfectHubPluginBuilder: HubPluginJsonBuilder
  get() = HubPluginJsonBuilder()

fun HubPluginJsonBuilder.modify(block: HubPluginJsonBuilder.() -> Unit): String {
  val copy = copy()
  copy.block()
  return copy.asString()
}