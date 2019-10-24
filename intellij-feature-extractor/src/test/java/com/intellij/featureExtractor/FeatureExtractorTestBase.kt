package com.intellij.featureExtractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.plugin.structure.classes.resolvers.DirectoryResolver
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.jdom2.Element
import org.junit.After
import org.junit.Before
import java.nio.file.Paths

abstract class FeatureExtractorTestBase {

  protected lateinit var resolver: Resolver
  protected lateinit var plugin: MockIdePlugin

  @Before
  fun setUp() {
    val fileOrigin = object : FileOrigin {
      override val parent: FileOrigin? = null
    }
    resolver = DirectoryResolver(Paths.get("."), fileOrigin)
    plugin = MockIdePlugin("pluginId", "1.0")
  }

  @After
  fun tearDown() {
    resolver.close()
  }

  fun resetPluginExtensionPoint(extensionPoint: ExtensionPoint, implementorName: String) {
    plugin.extensions.clear()
    val element = Element(extensionPoint.extensionPointName)
    element.setAttribute("implementation", implementorName)
    plugin.extensions.put(extensionPoint.extensionPointName, element)
  }

}