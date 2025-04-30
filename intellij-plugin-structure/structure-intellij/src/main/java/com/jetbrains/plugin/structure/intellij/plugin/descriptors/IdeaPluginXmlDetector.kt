package com.jetbrains.plugin.structure.intellij.plugin.descriptors

import com.jetbrains.plugin.structure.xml.XmlInputFactoryResult
import com.jetbrains.plugin.structure.xml.createXmlInputFactory
import java.nio.file.Path

fun interface IdeaPluginXmlDetector {
  fun isPluginDescriptor(descriptorPath: Path): Boolean
}

object NegativeIdeaPluginXmlDetector : IdeaPluginXmlDetector {
  override fun isPluginDescriptor(descriptorPath: Path) = false
}

fun createIdeaPluginXmlDetector(): IdeaPluginXmlDetector {
  return when (val result = createXmlInputFactory()) {
    is XmlInputFactoryResult.Created -> StaxIdeaPluginXmlDetector(result.xmlInputFactory)
    else -> NegativeIdeaPluginXmlDetector
  }
}