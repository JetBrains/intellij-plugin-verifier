/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MavenInstallRecipe(
  @JsonProperty(MAVEN_REPOSITORIES)
  val repositories: List<MavenRepository> = emptyList(),
  @JsonProperty(MAVEN_PLUGINS)
  val plugins: List<MavenPlugin> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BuildSystemDependency(
  @JsonProperty(DEPENDENCY_GROUP)
  val group: String? = null,
  @JsonProperty(DEPENDENCY_ARTIFACT)
  val artifact: String? = null,
  @JsonProperty(DEPENDENCY_VERSION)
  val version: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MavenRepository(
  @JsonProperty(MAVEN_REP_ID)
  val id: String? = null,
  @JsonProperty(MAVEN_REP_URL)
  val url: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MavenPlugin(
  @JsonProperty(PLUGIN_GROUP)
  val group: String? = null,
  @JsonProperty(PLUGIN_ARTIFACT)
  val artifact: String? = null,
  @JsonProperty(PLUGIN_VERSION)
  val version: String? = null
)