package com.jetbrains.intellij.feature.extractor

import com.google.gson.annotations.SerializedName

data class ExtractorResult(val features: List<ExtensionPointFeatures>, val extractedAll: Boolean)

data class ExtensionPointFeatures(@SerializedName("extensionPoint") val extensionPoint: ExtensionPoint,
                                  @SerializedName("implementorName") val epImplementorName: String,
                                  @SerializedName("featureNames") val featureNames: List<String>)

enum class ExtensionPoint(val extensionPointName: String) {
  CONFIGURATION_TYPE("com.intellij.configurationType"),
  FACET_TYPE("com.intellij.facetType"),
  FILE_TYPE("com.intellij.fileTypeFactory"),
  ARTIFACT_TYPE("com.intellij.packaging.artifactType")
  //TODO: module type: see https://plugins.jetbrains.com/plugin/9238 for example
}