package org.jetbrains.plugins.verifier.service.service.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskResult

data class FeaturesResult(val updateInfo: UpdateInfo,
                          val resultType: ResultType,
                          val features: List<ExtensionPointFeatures>) : ServiceTaskResult {
  enum class ResultType {
    NOT_FOUND,
    BAD_PLUGIN,
    EXTRACTED_ALL,
    EXTRACTED_PARTIALLY
  }
}