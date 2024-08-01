package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import org.jdom2.Element

class MockExtension(private val fullyQualifiedName: String, private val elements: List<Element>) {
  fun apply(plugin: IdePluginImpl) {
    plugin.extensions[fullyQualifiedName] = elements.toMutableList()
  }

  companion object {
    fun from(fullyQualifiedName: String, vararg attributes: Pair<String, String>): MockExtension {
      val element = Element(fullyQualifiedName).apply {
        for ((attrName, attrValue) in attributes) {
          setAttribute(attrName, attrValue)
        }
      }
      return MockExtension(fullyQualifiedName, listOf(element))
    }
  }
}
