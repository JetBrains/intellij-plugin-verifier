package com.intellij.featureExtractor

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.io.File

data class MockIdePlugin(
  override val pluginId: String?,
  override val pluginVersion: String?
) : IdePlugin {
  override val extensions: MutableMap<String, MutableList<Element>> = hashMapOf()
  override val pluginName: String? = null
  override val description: String? = null
  override val url: String? = null
  override val vendor: String? = null
  override val vendorEmail: String? = null
  override val vendorUrl: String? = null
  override val changeNotes: String? = null
  override val icons: List<PluginIcon> = emptyList()
  override val productDescriptor: ProductDescriptor? = null
  override val dependencies: List<PluginDependency> = emptyList()
  override val underlyingDocument: Document = Document(Element("idea-plugin"))
  override val optionalDescriptors: List<OptionalPluginDescriptor> = emptyList()
  override val sinceBuild: IdeVersion = IdeVersion.createIdeVersion("IU-163.1")
  override val untilBuild: IdeVersion? = null
  override val definedModules: Set<String> = emptySet()
  override val originalFile: File? = null
  override val useIdeClassLoader = false
  override val declaredThemes = emptyList<IdeTheme>()
  override fun isCompatibleWithIde(ideVersion: IdeVersion) = false
}