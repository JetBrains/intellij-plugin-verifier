package com.jetbrains.intellij.feature.extractor

import com.google.gson.annotations.SerializedName

data class ExtractorResult(val features: List<FeatureImplementation>, val extractedAll: Boolean)

data class FeatureImplementation(@SerializedName("feature") val feature: Feature,
                                 @SerializedName("implementor") val implementor: String,
                                 @SerializedName("featureNames") val featureNames: List<String>)

enum class Feature(val extensionPointName: String) {
  ConfigurationType("com.intellij.configurationType"),
  FacetType("com.intellij.facetType"),
  FileType("com.intellij.fileTypeFactory"),
  ArtifactType("com.intellij.packaging.artifactType")
  //TODO: module type: see https://plugins.jetbrains.com/plugin/9238 for example
}