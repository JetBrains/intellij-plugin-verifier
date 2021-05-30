/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class KtorVendor(
  @JsonProperty(VENDOR_NAME)
  val name: String? = null,
  @JsonProperty(VENDOR_EMAIL)
  val vendorEmail: String? = null,
  @JsonProperty(VENDOR_URL)
  val vendorUrl: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KtorFeatureDocumentation(
  @JsonProperty(DOCUMENTATION_DESCRIPTION)
  val description: String? = null,
  @JsonProperty(DOCUMENTATION_USAGE)
  val usage: String? = null,
  @JsonProperty(DOCUMENTATION_OPTIONS)
  val options: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KtorFeatureDescriptor(
  @JsonProperty(ID)
  val pluginId: String? = null,
  @JsonProperty(NAME)
  val pluginName: String? = null,
  @JsonProperty(VERSION)
  val pluginVersion: String? = null,
  @JsonProperty(KTOR_VERSION)
  val ktorVersion: KtorFeatureVersionDescriptor? = null,
  @JsonProperty(SHORT_DESCRIPTION)
  val shortDescription: String? = null,
  @JsonProperty(COPYRIGHT)
  val copyright: String? = null,
  @JsonProperty(VENDOR)
  val vendor: KtorVendor? = null,
  @JsonProperty(REQUIRED_FEATURES)
  val requiredFeatures: List<String> = emptyList(), // Feature IDs.
  @JsonProperty(INSTALL_RECEIPT)
  val installRecipe: FeatureInstallRecipe? = null,
  @JsonProperty(GRADLE_INSTALL)
  val gradleInstall: GradleInstallRecipe? = null,
  @JsonProperty(MAVEN_INSTALL)
  val mavenInstall: MavenInstallRecipe? = null,
  @JsonProperty(DEPENDENCIES)
  val dependencies: List<BuildSystemDependency> = emptyList(),
  @JsonProperty(TEST_DEPENDENCIES)
  val testDependencies: List<BuildSystemDependency> = emptyList(),
  @JsonProperty(DOCUMENTATION)
  val documentation: KtorFeatureDocumentation? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KtorFeatureVersionDescriptor(
  @JsonProperty(SINCE)
  val since: String?,
  @JsonProperty(UNTIL)
  val until: String?
)