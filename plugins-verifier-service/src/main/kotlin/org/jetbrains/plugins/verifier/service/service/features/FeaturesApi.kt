package org.jetbrains.plugins.verifier.service.service.features

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.pluginverifier.repository.UpdateInfo

fun prepareFeaturesResponse(updateInfo: UpdateInfo,
                            resultType: FeaturesResult.ResultType,
                            features: List<ExtensionPointFeatures>): String {
  val updateId = updateInfo.updateId
  val apiResultType = convertResultType(resultType)
  val apiFeatures = convertFeatures(features)
  return Gson().toJson(ApiFeaturesResult(updateId, apiResultType, apiFeatures))
}

private fun convertFeatures(features: List<ExtensionPointFeatures>): List<ApiExtensionPointFeatures> {
  return features.map {
    val apiExtensionPoint = convertExtensionPoint(it)
    ApiExtensionPointFeatures(apiExtensionPoint, it.epImplementorName, it.featureNames)
  }
}

private fun convertExtensionPoint(it: ExtensionPointFeatures): ApiExtensionPoint = when (it.extensionPoint) {
  ExtensionPoint.CONFIGURATION_TYPE -> ApiExtensionPoint.CONFIGURATION_TYPE
  ExtensionPoint.FACET_TYPE -> ApiExtensionPoint.FACET_TYPE
  ExtensionPoint.FILE_TYPE -> ApiExtensionPoint.FILE_TYPE
  ExtensionPoint.ARTIFACT_TYPE -> ApiExtensionPoint.ARTIFACT_TYPE
  ExtensionPoint.MODULE_TYPE -> ApiExtensionPoint.MODULE_TYPE
}

private fun convertResultType(resultType: FeaturesResult.ResultType): ApiFeaturesResult.ResultType = when (resultType) {
  FeaturesResult.ResultType.NOT_FOUND -> ApiFeaturesResult.ResultType.NOT_FOUND
  FeaturesResult.ResultType.BAD_PLUGIN -> ApiFeaturesResult.ResultType.BAD_PLUGIN
  FeaturesResult.ResultType.EXTRACTED_ALL -> ApiFeaturesResult.ResultType.EXTRACTED_ALL
  FeaturesResult.ResultType.EXTRACTED_PARTIALLY -> ApiFeaturesResult.ResultType.EXTRACTED_PARTIALLY
}


private data class ApiExtensionPointFeatures(@SerializedName("extensionPoint") val extensionPoint: ApiExtensionPoint,
                                             @SerializedName("implementorName") val epImplementorName: String,
                                             @SerializedName("featureNames") val featureNames: List<String>)

private data class ApiFeaturesResult(@SerializedName("updateId") val updateId: Int,
                                     @SerializedName("resultType") val resultType: ResultType,
                                     @SerializedName("features") val features: List<ApiExtensionPointFeatures> = emptyList()) {
  enum class ResultType {
    NOT_FOUND,
    BAD_PLUGIN,
    EXTRACTED_ALL,
    EXTRACTED_PARTIALLY
  }
}

private enum class ApiExtensionPoint(val extensionPointName: String) {
  CONFIGURATION_TYPE("com.intellij.configurationType"),
  FACET_TYPE("com.intellij.facetType"),
  FILE_TYPE("com.intellij.fileTypeFactory"),
  ARTIFACT_TYPE("com.intellij.packaging.artifactType"),
  MODULE_TYPE("com.intellij.moduleType")
}