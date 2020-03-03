package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element

import java.io.File

interface IdePlugin : Plugin {
  val sinceBuild: IdeVersion?

  val untilBuild: IdeVersion?

  val extensions: Map<String, List<Element>>

  val dependencies: List<PluginDependency>

  val definedModules: Set<String>

  val optionalDescriptors: List<OptionalPluginDescriptor>

  val underlyingDocument: Document

  val originalFile: File?

  val productDescriptor: ProductDescriptor?

  val declaredThemes: List<IdeTheme>

  val useIdeClassLoader: Boolean

  fun isCompatibleWithIde(ideVersion: IdeVersion): Boolean
}
