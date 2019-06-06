package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.FileTypeExtractor
import com.jetbrains.intellij.feature.extractor.extractor.FileTypeFactoryExtractor
import org.jdom2.Element
import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeTest : FeatureExtractorTestBase() {

  @Test
  fun fileType() {
    val extensions = "one;two;three"
    val expectedExtensions = FileTypeFactoryExtractor.parseExtensionsList(extensions)

    val element = Element(ExtensionPoint.FILE_TYPE.extensionPointName)
    element.setAttribute("extensions", extensions)
    plugin.extensions.put(ExtensionPoint.FILE_TYPE.extensionPointName, element)

    val features = FileTypeExtractor().extract(plugin, resolver)
    assertEquals(
        listOf(ExtensionPointFeatures(ExtensionPoint.FILE_TYPE, expectedExtensions)),
        features
    )
  }

}