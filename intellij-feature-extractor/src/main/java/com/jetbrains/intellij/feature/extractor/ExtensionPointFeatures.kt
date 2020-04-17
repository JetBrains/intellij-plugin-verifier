/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.intellij.feature.extractor

/**
 * Extension point used by plugins to specify
 * additional supported features.
 */
enum class ExtensionPoint(val extensionPointName: String) {
  CONFIGURATION_TYPE("com.intellij.configurationType"),
  FACET_TYPE("com.intellij.facetType"),
  FILE_TYPE_FACTORY("com.intellij.fileTypeFactory"),
  FILE_TYPE("com.intellij.fileType"),
  ARTIFACT_TYPE("com.intellij.packaging.artifactType"),
  MODULE_TYPE("com.intellij.moduleType")
}

/**
 * Holds all features of a single plugin's extension point.
 *
 * E.g. `com.intellij.openapi.fileTypes.FileTypeFactory` allows to specify multiple supported file types in one instance.
 */
data class ExtensionPointFeatures(
  /**
   * Extension point which allows to specify custom plugin implementation
   * of the IntelliJ API class for a specific feature type
   */
  val extensionPoint: ExtensionPoint,
  /**
   * Extracted feature name:
   *
   * For configuration type it is the ID of the configuration (e.g. JUnit)
   *
   * For facet types it is the ID of the facet (e.g. django)
   *
   * For file types it is the file extension pattern (e.g. '*.php', '*.scala')
   *
   * For artifact type it is the ID of the artifact (e.g. war, apk)
   */
  val featureNames: List<String>
)