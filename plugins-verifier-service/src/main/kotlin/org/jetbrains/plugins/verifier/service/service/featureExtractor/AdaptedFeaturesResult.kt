package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.google.gson.annotations.SerializedName
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures

data class AdaptedFeaturesResult(@SerializedName("updateId") val updateId: Int,
                                 @SerializedName("resultType") val resultType: FeaturesResult.ResultType,
                                 @SerializedName("features") val features: List<ExtensionPointFeatures> = emptyList(),
                                 @SerializedName("protocolVersion") val protocolVersion: Int)