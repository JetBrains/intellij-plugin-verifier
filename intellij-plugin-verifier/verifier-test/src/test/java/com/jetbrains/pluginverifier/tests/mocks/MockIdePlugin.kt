package com.jetbrains.pluginverifier.tests.mocks

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.io.File

data class MockIdePlugin(
  override val pluginId: String? = null,
  override val pluginName: String? = pluginId,
  override val pluginVersion: String? = null,
  override val description: String? = null,
  override val url: String? = null,
  override val vendor: String? = null,
  override val vendorEmail: String? = null,
  override val vendorUrl: String? = null,
  override val changeNotes: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val productDescriptor: ProductDescriptor? = null,
  override val dependencies: List<PluginDependency> = emptyList(),
  override val underlyingDocument: Document = Document(Element("idea-plugin")),
  override val optionalDescriptors: List<OptionalPluginDescriptor> = emptyList(),
  override val extensions: Multimap<String, Element> = HashMultimap.create(),
  override val sinceBuild: IdeVersion = IdeVersion.createIdeVersion("IU-163.1"),
  override val untilBuild: IdeVersion? = null,
  override val definedModules: Set<String> = emptySet(),
  override val originalFile: File? = null
) : IdePlugin {

  override val useIdeClassLoader = false

  override val declaredThemes = emptyList<IdeTheme>()

  override fun isCompatibleWithIde(ideVersion: IdeVersion) =
    sinceBuild <= ideVersion && (untilBuild == null || ideVersion <= untilBuild)
}