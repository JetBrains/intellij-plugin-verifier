package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.FacetTypeExtractor
import org.junit.Assert
import org.junit.Test

class FacetTypeTests : FeatureExtractorTestBase() {
  @Test
  fun constant() {
    assertExtractFacets("featureExtractor.facetType.Constant", listOf("thisIsStringId"))
  }

  @Test
  fun constant2() {
    assertExtractFacets("featureExtractor.facetType.Constant2", listOf("thisIsStringId2"))
  }


  @Test
  fun finalField() {
    assertExtractFacets("featureExtractor.facetType.FinalField", listOf("thisIsStringId"))
  }

  private fun assertExtractFacets(className: String, expectedFacetTypes: List<String>) {
    resetPluginExtensionPoint(ExtensionPoint.FACET_TYPE, className)
    val extractor = FacetTypeExtractor()
    val features = extractor.extract(plugin, resolver)
    Assert.assertEquals(
        listOf(ExtensionPointFeatures(ExtensionPoint.FACET_TYPE, expectedFacetTypes)),
        features
    )
  }

}
