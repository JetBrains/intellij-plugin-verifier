package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

class DependencySupportExtractor : Extractor {
  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    val extensionElements = plugin.extensions[ExtensionPoint.DEPENDENCY_SUPPORT_TYPE.extensionPointName] ?: return emptyList()
    return extensionElements.mapNotNull { element ->
      val kind = element.getAttributeValue("kind") ?: return@mapNotNull null
      val coordinate = element.getAttributeValue("coordinate") ?: return@mapNotNull null
      ExtensionPointFeatures(ExtensionPoint.DEPENDENCY_SUPPORT_TYPE, listOf("$kind:$coordinate"))
    }
  }
}