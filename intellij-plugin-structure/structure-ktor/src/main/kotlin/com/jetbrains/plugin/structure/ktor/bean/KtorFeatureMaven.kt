/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MavenInstallRecipe(
  @SerialName(MAVEN_REPOSITORIES)
  val repositories: List<MavenRepository> = emptyList(),
  @SerialName(MAVEN_PLUGINS)
  val plugins: List<MavenPlugin> = emptyList(),
  @SerialName(MAVEN_DEPENDENCIES)
  val dependencies: List<BuildSystemDependency> = emptyList(),
  @SerialName(MAVEN_TEST_DEPENDENCIES)
  val testDependencies: List<BuildSystemDependency> = emptyList()
)

@Serializable
data class BuildSystemDependency(
  @SerialName(DEPENDENCY_GROUP)
  val group: String? = null,
  @SerialName(DEPENDENCY_ARTIFACT)
  val artifact: String? = null,
  @SerialName(DEPENDENCY_VERSION)
  val version: String? = null
)

@Serializable
data class MavenRepository(
  @SerialName(MAVEN_REP_ID)
  val id: String? = null,
  @SerialName(MAVEN_REP_URL)
  val url: String? = null
)

@Serializable
data class MavenPlugin(
  @SerialName(PLUGIN_GROUP)
  val group: String? = null,
  @SerialName(PLUGIN_ARTIFACT)
  val artifact: String? = null,
  @SerialName(PLUGIN_VERSION)
  val version: String? = null
)