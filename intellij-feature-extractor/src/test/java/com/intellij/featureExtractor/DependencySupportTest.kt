package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.intellij.feature.extractor.extractor.DependencySupportExtractor
import org.jdom2.Element
import org.junit.Assert.assertEquals
import org.junit.Test

class DependencySupportTest : FeatureExtractorTestBase() {

  @Suppress("SameParameterValue")
  private fun assertDependencySupportFound(expectedFeatureNames: List<String>, kind: String, coordinate: String) {
    plugin.extensions.clear()
    val element = Element(ExtensionPoint.DEPENDENCY_SUPPORT_TYPE.extensionPointName)
    element.setAttribute("kind", kind)
    element.setAttribute("coordinate", coordinate)
    plugin.extensions.getOrPut(ExtensionPoint.DEPENDENCY_SUPPORT_TYPE.extensionPointName) { arrayListOf() } += element
    val features = DependencySupportExtractor().extract(plugin, resolver)
    assertEquals(listOf(ExtensionPointFeatures(ExtensionPoint.DEPENDENCY_SUPPORT_TYPE, expectedFeatureNames)), features)
  }

  @Test
  fun `junit junit`() {
    assertDependencySupportFound(listOf("java:junit:junit"), "java", "junit:junit")
  }
}