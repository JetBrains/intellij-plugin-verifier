package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.pluginverifier.api.PluginInfo

data class FeaturesResult(val plugin: PluginInfo,
                          val resultType: ResultType,
                          val features: List<ExtensionPointFeatures>) {
  enum class ResultType {
    NOT_FOUND,
    BAD_PLUGIN,
    EXTRACTED_ALL,
    EXTRACTED_PARTIALLY
  }
}