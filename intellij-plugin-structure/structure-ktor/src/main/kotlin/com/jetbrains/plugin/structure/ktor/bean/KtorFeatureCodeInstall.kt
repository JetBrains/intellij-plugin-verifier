/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeatureInstallRecipe(
  @JsonProperty(INSTALL_IMPORTS)
  val imports: List<String> = emptyList(),
  @JsonProperty(INSTALL_TEST_IMPORTS)
  val testImports: List<String> = emptyList(),
  @JsonProperty(INSTALL_BLOCK)
  val installBlock: String? = null,
  @JsonProperty(INSTALL_TEMPLATES)
  val templates: List<CodeTemplate> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
enum class Position {
  @JsonProperty(POSITION_INSIDE)
  INSIDE_APPLICATION_MODULE,

  @JsonProperty(POSITION_OUTSIDE)
  OUTSIDE_APPLICATION_MODULE,

  @JsonProperty(POSITION_ROUTING)
  IN_ROUTING_BLOCK,

  @JsonProperty(POSITION_SERIALIZATION)
  IN_SERIALIZATION_BLOCK,

  @JsonProperty(POSITION_TESTFUN)
  TEST_FUNCTION,

  @JsonProperty(RESOURCES_FILE)
  RESOURCES,

  @JsonProperty(IN_CINFIG)
  CONFIG
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeTemplate(
  @JsonProperty(TEMPLATE_POSITION)
  val position: Position? = null,
  @JsonProperty(TEMPLATE_NAME)
  val name: Position? = null,
  @JsonProperty(TEMPLATE_TEXT)
  val text: String? = null
)