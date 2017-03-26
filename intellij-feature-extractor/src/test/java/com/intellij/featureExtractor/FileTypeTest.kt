package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.core.FileTypeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class FileTypeTest : FeatureExtractorTestBase() {

  @Test
  fun fileTypeInstance() {
    assertExtractFileTypes("featureExtractor.fileType.ByFileTypeFactory", listOf("*.mySomeExtension"))
  }

  private fun assertExtractFileTypes(className: String, extensions: List<String>) {
    val node = readClassNode(className)
    val result = FileTypeExtractor(resolver).extract(node)
    val list = result.features
    assertTrue(result.extractedAll)
    assertEquals(extensions, list)
  }

  @Test
  fun nameMatcher() {
    assertExtractFileTypes("featureExtractor.fileType.MatcherFileTypeFactory", listOf("firstExactName", "secondExactName", "*.nmextension"))
  }


  @Test
  fun constantFileType() {
    assertExtractFileTypes("featureExtractor.fileType.ConstantFileTypeFactory", listOf("*..someExtension"))
  }

  @Test
  fun constantFunctionFileType() {
    assertExtractFileTypes("featureExtractor.fileType.ConstantFunctionFileTypeFactory", listOf("*..constantValue"))
  }

  @Test
  fun staticClinitConstant() {
    assertExtractFileTypes("featureExtractor.fileType.StaticInitConstantFileTypeFactory", listOf("*.one", "*.two", "*.three"))
  }

}