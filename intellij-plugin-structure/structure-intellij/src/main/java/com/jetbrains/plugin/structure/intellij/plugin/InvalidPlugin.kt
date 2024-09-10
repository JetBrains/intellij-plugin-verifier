package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

/**
 * An invalid plugin that was either not fully parsed or that has been parsed with syntax error in the descriptor.
 * Such placeholder class can be used in the verification engines to filter out specific verification rules
 * despite the incomplete or erroneous metadata from plugin descriptor.
 */
class InvalidPlugin(override val underlyingDocument: Document) : IdePlugin {
  override var pluginId: String? = null
  override var pluginName: String? = null
  override var pluginVersion: String? = null
  override var url: String? = null
  override var changeNotes: String? = null
  override var description: String? = null
  override var vendor: String? = null
  override var vendorEmail: String? = null
  override var vendorUrl: String? = null
  override var icons: List<PluginIcon> = emptyList()
  override var thirdPartyDependencies: List<ThirdPartyDependency> = emptyList()

  override val sinceBuild: IdeVersion? = null
  override val untilBuild: IdeVersion? = null
  override val extensions: Map<String, List<Element>> = emptyMap()
  override val appContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor()
  override val projectContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor()
  override val moduleContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor()
  override val dependencies: List<PluginDependency> = emptyList()
  override val incompatibleModules: List<String> = emptyList()
  override val definedModules: Set<String> = emptySet()
  override val optionalDescriptors: List<OptionalPluginDescriptor> = emptyList()
  override val modulesDescriptors: List<ModuleDescriptor> = emptyList()
  override val originalFile: Path? = null
  override val productDescriptor: ProductDescriptor? = null
  override val declaredThemes: List<IdeTheme> = emptyList()
  override val useIdeClassLoader: Boolean = false
  override val isImplementationDetail: Boolean = false
  override val isV2: Boolean = false
  override val ideMode: IdeMode = IdeMode.K1OnlyCompatible
  override fun isCompatibleWithIde(ideVersion: IdeVersion): Boolean = false
  override val hasDotNetPart: Boolean = false
}