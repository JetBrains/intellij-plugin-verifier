package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.RunConfigurationExtractor
import org.junit.Assert
import org.junit.Test

class ConfigurationTypeTest : FeatureExtractorTestBase() {
  @Test
  fun constantConfiguration() {
    assertExtractConfiguration("featureExtractor.configurationType.ConstantConfigurationType", listOf("runConfiguration"))
  }

  @Test
  fun baseConfiguration() {
    assertExtractConfiguration("featureExtractor.configurationType.ConfigurationTypeBaseImplementor", listOf("ConfigurationId"))
  }

  private fun assertExtractConfiguration(className: String, expectedConfigurationIds: List<String>) {
    resetPluginExtensionPoint(ExtensionPoint.CONFIGURATION_TYPE, className)
    val actual = RunConfigurationExtractor().extract(plugin, resolver)
    Assert.assertEquals(
        listOf(ExtensionPointFeatures(ExtensionPoint.CONFIGURATION_TYPE, expectedConfigurationIds)),
        actual
    )
  }

}
