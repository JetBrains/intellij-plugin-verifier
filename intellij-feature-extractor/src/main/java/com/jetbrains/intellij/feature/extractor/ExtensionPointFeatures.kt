package com.jetbrains.intellij.feature.extractor

import com.google.gson.annotations.SerializedName

/**
 * Holds all features of a single plugin's extension point.
 *
 * E.g. `com.intellij.openapi.fileTypes.FileTypeFactory` allows to specify
 * multiple supported file types in one instance.
 *
 * The plugin's extension point implementor class name is [epImplementorName] and
 * feature names is [featureNames]
 */
data class ExtensionPointFeatures(
    /**
     * Extension point which allows to specify custom plugin implementation
     * of the IntelliJ API class for a specific feature type
     */
    @SerializedName("extensionPoint") val extensionPoint: ExtensionPoint,
    /**
     * Class name of the plugin's implementation class
     */
    @SerializedName("implementorName") val epImplementorName: String,
    /**
     * Extracted feature name:
     *
     * For configuration type it is the ID of the configuration (e.g. JUnit)
     *
     * For facet types it is the ID of the facet (e.g. django)
     *
     * For file types it is the file extension pattern (e.g. '*.php', '*.scala')
     *
     * For artifact type it is the ID of the artifact (e.g. war, apk)
     */
    @SerializedName("featureNames") val featureNames: List<String>
)