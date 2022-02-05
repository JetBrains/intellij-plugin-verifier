/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetPluginDescriptor(
  val id: String? = null,
  val name: String? = null,
  val version: String? = null,
  val description: String? = null,
  val depends: Map<String, String>? = null,  //value in a form of "1.0.0" or "1.0.0+" todo deserialize to DependencyVersion
  val frontend: Part? = null,
  val workspace: Part? = null,
  val vendor: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
sealed class DependencyVersion {
  class CompatibleWith(val version: String) : DependencyVersion()
  class Above(val version: String) : DependencyVersion()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Part(
  //strings in modules and classpath in a form of filepath.ext:sha todo deserialize to Coordinate
  val modules: List<String>,
  val classpath: List<String>,
  val roots: List<String>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
sealed class Coordinate {
  class File(val relativePath: String, val sha: String) : Coordinate()
  class URL(val url: String, val sha: String) : Coordinate()
}
