/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * Protocol used to communicate with JetBrains Marketplace:
 * 1) Get plugins to extract plugin features: [getUpdatesToExtract]
 * 2) Send features extraction results: [sendExtractedFeatures]
 */
interface FeatureServiceProtocol {

  fun getUpdatesToExtract(): List<UpdateInfo>

  fun sendExtractedFeatures(extractFeaturesResult: ExtractFeaturesTask.Result)

}
