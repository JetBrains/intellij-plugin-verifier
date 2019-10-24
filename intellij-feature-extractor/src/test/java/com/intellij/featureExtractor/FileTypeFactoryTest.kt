package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.FileTypeFactoryExtractor
import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeFactoryTest : FeatureExtractorTestBase() {

  private fun assertExtractFileTypes(className: String, expectedExtensions: List<String>) {
    resetPluginExtensionPoint(ExtensionPoint.FILE_TYPE_FACTORY, className)
    val features = FileTypeFactoryExtractor().extract(plugin, resolver)
    assertEquals(
      listOf(ExtensionPointFeatures(ExtensionPoint.FILE_TYPE_FACTORY, expectedExtensions)),
      features
    )
  }

  @Test
  fun fileTypeInstance() {
    assertExtractFileTypes("featureExtractor.fileType.ByFileTypeFactory", listOf("*.mySomeExtension"))
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