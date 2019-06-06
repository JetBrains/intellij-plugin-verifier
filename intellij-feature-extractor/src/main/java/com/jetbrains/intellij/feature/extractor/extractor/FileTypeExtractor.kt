package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.intellij.feature.extractor.ExtensionPointFeatures
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

/**
 * Extracts <fileType> extensions registered in plugin.xml.
 *
 * 'FileTypeFactory' used to be the only way to supply supported file types to the platform.
 * Now the preferred way to do that is registration of the <fileType> extension in plugin.xml.
 */
class FileTypeExtractor : Extractor {

  override fun extract(plugin: IdePlugin, resolver: Resolver): List<ExtensionPointFeatures> {
    val extensionsElements = plugin.extensions[ExtensionPoint.FILE_TYPE.extensionPointName]
    val result = arrayListOf<ExtensionPointFeatures>()
    for (element in extensionsElements) {
      val extensions = element.getAttributeValue("extensions")
      if (extensions != null) {
        val extensionsList = FileTypeFactoryExtractor.parseExtensionsList(extensions)
        if (extensionsList.isNotEmpty()) {
          result += ExtensionPointFeatures(ExtensionPoint.FILE_TYPE, extensionsList)
        }
      }
    }
    return result
  }

}