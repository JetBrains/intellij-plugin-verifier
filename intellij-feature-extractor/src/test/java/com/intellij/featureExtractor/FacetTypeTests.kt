package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.core.FacetTypeExtractor
import org.junit.Assert
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
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

  private fun assertExtractFacets(className: String, listOf: List<String>) {
    val node = readClassNode(className)
    val extractor = FacetTypeExtractor(resolver)
    val facetTypeId = extractor.extract(node).features
    Assert.assertEquals(listOf, facetTypeId)
  }

}
