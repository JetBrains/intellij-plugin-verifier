/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class GradleInstallReceipt(
  val dependencies: List<Dependency> = emptyList(),
  val repositories: List<MavenRepository> = emptyList(),
  val plugins: List<MavenPlugin> = emptyList(),
)

data class GradleRepository(
  val type: GradleRepositoryType,
  val functionName: String? = null,
  val url: String? = null
)

enum class GradleRepositoryType(
  FUNCTION,
  URL
)

data class GradlePlugin(
  val id: String,
  val version: String? = null
)