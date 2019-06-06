package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory

fun getExtensionPointImplementors(plugin: IdePlugin, resolver: Resolver, extensionPoint: ExtensionPoint): List<ClassNode> {
  val extensionElements = plugin.extensions[extensionPoint.extensionPointName] ?: return emptyList()
  val result = arrayListOf<String>()
  extensionElements.mapNotNullTo(result) { it.getAttributeValue("implementation") }
  extensionElements.mapNotNullTo(result) { it.getAttributeValue("implementationClass") }
  return result.mapNotNull { resolver.findClassLogged(it) }
}

private val LOG = LoggerFactory.getLogger("FeaturesExtractor.ClassResolver")

fun Resolver.findClassLogged(className: String): ClassNode? {
  try {
    return findClass(className.replace('.', '/')) ?: return null
  } catch (e: Exception) {
    e.rethrowIfInterrupted()
    LOG.warn("Unable to get find class file '$className'", e)
    return null
  }
}