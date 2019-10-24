package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.ArtifactTypeExtractor
import org.junit.Assert
import org.junit.Test

class ArtifactTypeTest : FeatureExtractorTestBase() {

  private fun assertExtractArtifactType(className: String, expectedArtifactTypes: List<String>) {
    resetPluginExtensionPoint(ExtensionPoint.ARTIFACT_TYPE, className)
    val featuresList = ArtifactTypeExtractor().extract(plugin, resolver)
    Assert.assertEquals(
      listOf(ExtensionPointFeatures(ExtensionPoint.ARTIFACT_TYPE, expectedArtifactTypes)),
      featuresList
    )
  }

  @Test
  fun directArtifact() {
    assertExtractArtifactType("featureExtractor.artifactType.DirectArtifactType", listOf("ArtifactId"))
  }

  @Test
  fun indirectArtifact() {
    assertExtractArtifactType("featureExtractor.artifactType.IndirectArtifactType", listOf("IndirectArtifactId"))
  }
}