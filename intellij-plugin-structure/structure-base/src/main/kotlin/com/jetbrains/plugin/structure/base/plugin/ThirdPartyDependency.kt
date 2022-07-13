package com.jetbrains.plugin.structure.base.plugin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.base.utils.exists
import java.nio.file.Files
import java.nio.file.Path

data class ThirdPartyDependency(
  val licenseUrl: String?,
  val license: String?,
  val url: String?,
  val name: String,
  val version: String,
)

fun parseThirdPartyDependenciesByPath(path: Path): List<ThirdPartyDependency> {
  if (path.exists().not()) return emptyList()
  return runCatching {
    val dependencies: List<ThirdPartyDependency> = jacksonObjectMapper().readValue(Files.readAllBytes(path))
    dependencies
  }.getOrNull() ?: emptyList()
}