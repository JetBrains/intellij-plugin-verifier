package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.core.RunConfigurationExtractor
import org.junit.Assert
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class ConfigurationTypeTest : FeatureExtractorTestBase() {
  @Test
  fun constantConfiguration() {
    assertExtractConfiguration("featureExtractor.configurationType.ConstantConfigurationType", "runConfiguration")
  }

  @Test
  fun baseConfiguration() {
    assertExtractConfiguration("featureExtractor.configurationType.ConfigurationTypeBaseImplementor", "ConfigurationId")
  }

  private fun assertExtractConfiguration(className: String, configuration: String) {
    val node = readClassNode(className)
    val actual = RunConfigurationExtractor(resolver).extract(node).featureNames
    Assert.assertEquals(listOf(configuration), actual)
  }

}
