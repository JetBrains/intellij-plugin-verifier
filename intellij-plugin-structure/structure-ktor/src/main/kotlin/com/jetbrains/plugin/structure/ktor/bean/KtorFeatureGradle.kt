/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GradleInstallRecipe(
  @JsonProperty(GRADLE_REPOSITORIES)
  val repositories: List<GradleRepository> = emptyList(),
  @JsonProperty(GRADLE_PLUGINS)
  val plugins: List<GradlePlugin> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GradleRepository(
  @JsonProperty(GRADLE_REP_TYPE)
  val type: GradleRepositoryType? = null,
  @JsonProperty(GRADLE_REP_FUNCTION)
  val functionName: String? = null,
  @JsonProperty(GRADLE_REP_URL)
  val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
enum class GradleRepositoryType {
  @JsonProperty(GRADLE_REP_TYPE_FUNCTION)
  FUNCTION,

  @JsonProperty(GRADLE_REP_TYPE_URL)
  URL
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GradlePlugin(
  @JsonProperty(PLUGIN_ID)
  val id: String? = null,
  @JsonProperty(PLUGIN_VERSION)
  val version: String? = null
)