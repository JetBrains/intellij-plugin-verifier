/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradleInstallReceipt(
  @SerialName(GRADLE_DEPENDENCIES)
  val dependencies: List<BuildSystemDependency> = emptyList(),
  @SerialName(GRADLE_REPOSITORIES)
  val repositories: List<MavenRepository> = emptyList(),
  @SerialName(GRADLE_PLUGINS)
  val plugins: List<GradlePlugin> = emptyList(),
)

@Serializable
data class GradleRepository(
  @SerialName(GRADLE_REP_TYPE)
  val type: GradleRepositoryType? = null,
  @SerialName(GRADLE_REP_FUNCTION)
  val functionName: String? = null,
  @SerialName(GRADLE_REP_URL)
  val url: String? = null
)

@Serializable
enum class GradleRepositoryType(
  @SerialName(GRADLE_REP_TYPE_FUNCTION)
  FUNCTION,
  @SerialName(GRADLE_REP_TYPE_URL)
  URL
)

@Serializable
data class GradlePlugin(
  @SerialName(PLUGIN_ID)
  val id: String? = null,
  @SerialName(PLUGIN_VERSION)
  val version: String? = null
)