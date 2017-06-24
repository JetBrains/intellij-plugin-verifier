package com.jetbrains.intellij.feature.extractor

data class ExtractorResult(
    /**
     * Extracted features list. May be incomplete.
     */
    val features: List<ExtensionPointFeatures>,
    /**
     * Whether the feature extractor analysis is powerful enough
     * for extracting all plugin's features.
     *
     * If true all features are extracted, otherwise there is some tricky case
     * which is not proceed.
     */
    val extractedAll: Boolean
)

