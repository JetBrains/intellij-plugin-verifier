package com.jetbrains.intellij.feature.extractor.extractor

import com.jetbrains.intellij.feature.extractor.ExtensionPoint
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull

fun getExtensionPointImplementors(plugin: IdePlugin, resolver: Resolver, extensionPoint: ExtensionPoint): List<ClassFile> {
  val extensionElements = plugin.extensions[extensionPoint.extensionPointName] ?: return emptyList()
  val result = arrayListOf<String>()
  extensionElements.mapNotNullTo(result) { it.getAttributeValue("implementation") }
  extensionElements.mapNotNullTo(result) { it.getAttributeValue("implementationClass") }
  return result.mapNotNull { resolver.resolveClassOrNull(it.replace('.', '/')) }
}