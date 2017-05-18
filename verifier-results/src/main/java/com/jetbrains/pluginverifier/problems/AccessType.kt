package com.jetbrains.pluginverifier.problems

enum class AccessType(private val type: String) {
  PUBLIC("public"),
  PROTECTED("protected"),
  PACKAGE_PRIVATE("package-private"),
  PRIVATE("private");

  override fun toString(): String = type
}