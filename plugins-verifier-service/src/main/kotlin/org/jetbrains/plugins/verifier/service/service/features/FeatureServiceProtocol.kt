package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.pluginverifier.repository.UpdateInfo

interface FeatureServiceProtocol {

  fun getUpdatesToExtract(): List<UpdateInfo>

  fun sendExtractedFeatures(extractFeaturesResult: ExtractFeaturesTask.Result)

}