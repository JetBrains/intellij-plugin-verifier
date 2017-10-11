package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.presentation.Presentable

enum class AccessType(private val type: String) : Presentable {
  PUBLIC("public"),
  PROTECTED("protected"),
  PACKAGE_PRIVATE("package-private"),
  PRIVATE("private");

  override fun toString(): String = type

  override val shortPresentation: String = type

  override val fullPresentation: String = type
}