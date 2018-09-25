package com.jetbrains.pluginverifier.tests.mocks

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
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
    private val dependencies: List<PluginDependency> = emptyList(),
    private val underlyingDocument: Document = Document(Element("idea-plugin")),
    private val optionalDescriptors: Map<String, IdePlugin> = emptyMap(),
    private val extensions: Multimap<String, Element> = HashMultimap.create(),
    private val sinceBuild: IdeVersion = IdeVersion.createIdeVersion("IU-163.1"),
    private val untilBuild: IdeVersion? = null,
    private val definedModules: Set<String> = emptySet(),
    private val originalFile: File? = null
) : IdePlugin {

  override fun isCompatibleWithIde(ideVersion: IdeVersion) =
      sinceBuild <= ideVersion && (untilBuild == null || ideVersion <= untilBuild)

  override fun getUntilBuild() = untilBuild

  override fun getSinceBuild() = sinceBuild

  override fun getDefinedModules() = definedModules

  override fun getOriginalFile() = originalFile

  override fun getDependencies() = dependencies

  override fun getExtensions() = extensions

  override fun getOptionalDescriptors() = optionalDescriptors

  override fun getUnderlyingDocument() = underlyingDocument

}