package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * Protocol used to communicate with the Marketplace:
 * 1) Get plugins to extract plugin features: [getUpdatesToExtract]
 * 2) Send features extraction results: [sendExtractedFeatures]
 */
interface FeatureServiceProtocol {

  fun getUpdatesToExtract(): List<UpdateInfo>

  fun sendExtractedFeatures(extractFeaturesResult: ExtractFeaturesTask.Result)

}