package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import org.jdom2.Element

class MockExtension(private val fullyQualifiedName: String, private val elements: List<Element>) {
  fun apply(plugin: IdePluginImpl) {
    plugin.extensions[fullyQualifiedName] = elements.toMutableList()
  }

  companion object {
    fun from(extensionLocalName: String, vararg attributes: Pair<String, String>): MockExtension {
      val extensionFqn = "com.intellij.$extensionLocalName"
      val element = Element(extensionFqn).apply {
        for ((attrName, attrValue) in attributes) {
          setAttribute(attrName, attrValue)
        }
      }
      return MockExtension(extensionFqn, listOf(element))
    }
  }
}
