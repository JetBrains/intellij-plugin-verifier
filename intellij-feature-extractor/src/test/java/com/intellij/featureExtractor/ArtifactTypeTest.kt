package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.core.ArtifactTypeExtractor
import org.junit.Assert
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class ArtifactTypeTest : FeatureExtractorTestBase() {

  private fun assertExtractArtifactType(className: String, artifactTypes: List<String>) {
    val node = readClassNode(className)
    val list = ArtifactTypeExtractor(resolver).extract(node).features
    Assert.assertEquals(artifactTypes, list)
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