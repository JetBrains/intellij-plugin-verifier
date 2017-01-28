package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.FileTypeExtractor
import org.junit.Assert.assertEquals
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
    val list = FileTypeExtractor(resolver).extract(node).features
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