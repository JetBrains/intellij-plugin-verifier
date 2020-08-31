/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MavenInstallReceipt(
  val dependencies: List<Dependency> = emptyList(),
  val repositories: List<MavenRepository> = emptyList(),
  val plugins: List<MavenPlugin> = emptyList(),
)

data class BuildSystemDependency(
  val group: String,
  val artifact: String,
  val version: String? = null,
)

data class MavenRepository(
  val id: String,
  val url: String
)

data class MavenPlugin(
  val group: String,
  val artifact: String,
  val version: String
)