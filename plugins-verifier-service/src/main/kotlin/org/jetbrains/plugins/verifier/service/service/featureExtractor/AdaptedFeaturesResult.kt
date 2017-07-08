package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.google.gson.annotations.SerializedName

data class AdaptedFeaturesResult(@SerializedName("updateId") val updateId: Int,
                                 @SerializedName("resultType") val resultType: FeaturesResult.ResultType,
                                 @SerializedName("features") val features: List<Any> = emptyList(),
                                 @SerializedName("protocolVersion") val protocolVersion: Int)