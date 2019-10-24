package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.FileTypeExtractor
import org.jdom2.Element
import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeTest : FeatureExtractorTestBase() {

  @Test
  fun extensions() {
    val element = Element(ExtensionPoint.FILE_TYPE.extensionPointName)
    plugin.extensions.put(ExtensionPoint.FILE_TYPE.extensionPointName, element)
    element.setAttribute("extensions", "one;two;three")

    val expectedExtensions = listOf("*.one", "*.two", "*.three")
    val features = FileTypeExtractor().extract(plugin, resolver)
    assertEquals(
      listOf(ExtensionPointFeatures(ExtensionPoint.FILE_TYPE, expectedExtensions)),
      features
    )
  }

  @Test
  fun `file names`() {
    val element = Element(ExtensionPoint.FILE_TYPE.extensionPointName)
    element.setAttribute("fileNames", ".NameOne;nameTwo.txt;ExAcTNameThree.example")
    plugin.extensions.put(ExtensionPoint.FILE_TYPE.extensionPointName, element)
    val features = FileTypeExtractor().extract(plugin, resolver)
    assertEquals(
      listOf(ExtensionPointFeatures(ExtensionPoint.FILE_TYPE, listOf(".NameOne", "nameTwo.txt", "ExAcTNameThree.example"))),
      features
    )
  }

  @Test
  fun `case insensitive file names`() {
    val element = Element(ExtensionPoint.FILE_TYPE.extensionPointName)
    plugin.extensions.put(ExtensionPoint.FILE_TYPE.extensionPointName, element)
    element.setAttribute("fileNamesCaseInsensitive", ".nameone;nametwo.txt;exactnametree.example")
    val features = FileTypeExtractor().extract(plugin, resolver)
    assertEquals(
      listOf(ExtensionPointFeatures(ExtensionPoint.FILE_TYPE, listOf(".nameone", "nametwo.txt", "exactnametree.example"))),
      features
    )
  }

  @Test
  fun `file patterns`() {
    val element = Element(ExtensionPoint.FILE_TYPE.extensionPointName)
    plugin.extensions.put(ExtensionPoint.FILE_TYPE.extensionPointName, element)
    element.setAttribute("patterns", "*.js.flow")
    val features = FileTypeExtractor().extract(plugin, resolver)
    assertEquals(
      listOf(ExtensionPointFeatures(ExtensionPoint.FILE_TYPE, listOf("*.js.flow"))),
      features
    )
  }

}