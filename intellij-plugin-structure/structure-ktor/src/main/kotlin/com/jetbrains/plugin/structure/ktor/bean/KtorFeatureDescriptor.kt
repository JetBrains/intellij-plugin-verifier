/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KtorVendor(
  @SerialName(VENDOR_NAME)
  val name: String? = null,
  @SerialName(VENDOR_EMAIL)
  val vendorEmail: String? = null,
  @SerialName(VENDOR_URL)
  val vendorUrl: String? = null
)

@Serializable
data class KtorFeatureDocumentation(
  @SerialName(DOCUMENTATION_DESCRIPTION)
  val description: String? = null,
  @SerialName(DOCUMENTATION_USAGE)
  val usage: String? = null,
  @SerialName(DOCUMENTATION_OPTIONS)
  val options: String? = null
)

@Serializable
data class KtorFeatureDescriptor(
  @SerialName(ID)
  val pluginId: String? = null,
  @SerialName(NAME)
  val pluginName: String? = null,
  @SerialName(VERSION)
  val pluginVersion: String? = null,
  @SerialName(DESCRIPTION)
  val description: String? = null,
  @SerialName(COPYRIGHT)
  val copyright: String? = null,
  @SerialName(VENDOR)
  val vendor: KtorVendor? = null,
  @SerialName(REQUIRED_FEATURES)
  val requiredFeatures: List<String> = emptyList(), // Feature IDs.
  @SerialName(INSTALL_RECEIPT)
  val installReceipt: FeatureInstallReceipt? = null,
  @SerialName(TEST_INSTALL_RECEIPT)
  val testInstallReceipt: FeatureInstallReceipt? = null,
  @SerialName(GRADLE_INSTALL)
  val gradleInstall: GradleInstallReceipt? = null,
  @SerialName(MAVEN_INSTALL)
  val mavenInstall: MavenInstallReceipt? = null,
  @SerialName(DOCUMENTATION)
  val documentation: KtorFeatureDocumentation? = null
)