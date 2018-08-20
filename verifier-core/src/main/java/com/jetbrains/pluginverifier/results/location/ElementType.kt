package com.jetbrains.pluginverifier.results.location

/**
 * Type of an API element.
 */
enum class ElementType(val presentableName: String) {
  CLASS("class"),
  INTERFACE("interface"),
  ANNOTATION("annotation"),
  ENUM("enum"),
  METHOD("method"),
  CONSTRUCTOR("constructor"),
  FIELD("field");

  override fun toString() = presentableName
}