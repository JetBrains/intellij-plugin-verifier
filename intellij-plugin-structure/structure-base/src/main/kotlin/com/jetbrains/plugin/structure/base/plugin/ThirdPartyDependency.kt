package com.jetbrains.plugin.structure.base.plugin

data class ThirdPartyDependency(
  val licenseUrl: String?,
  val license: String?,
  val url: String?,
  val name: String,
  val version: String,
)