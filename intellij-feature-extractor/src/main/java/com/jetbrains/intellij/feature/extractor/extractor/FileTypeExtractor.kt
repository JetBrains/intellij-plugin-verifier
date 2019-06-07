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
    val features = arrayListOf<ExtensionPointFeatures>()
    for (element in extensionsElements) {
      val extensionsAndFileNames = arrayListOf<String>()
      extensionsAndFileNames += FileTypeFactoryExtractor.parseExtensionsList(element.getAttributeValue("extensions"))
      extensionsAndFileNames += FileTypeFactoryExtractor.splitSemicolonDelimitedList(element.getAttributeValue("fileNames"))
      extensionsAndFileNames += FileTypeFactoryExtractor.splitSemicolonDelimitedList(element.getAttributeValue("fileNamesCaseInsensitive"))
      if (extensionsAndFileNames.isNotEmpty()) {
        features += ExtensionPointFeatures(ExtensionPoint.FILE_TYPE, extensionsAndFileNames)
      }
    }
    return features
  }

}