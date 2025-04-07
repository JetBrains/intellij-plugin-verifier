package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.plugin.KotlinPluginMode
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.MutableIdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.OptionalPluginDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.ProductDescriptor
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

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
  override val incompatibleModules: List<String> = emptyList(),
  override val underlyingDocument: Document = Document(Element("idea-plugin")),
  override val optionalDescriptors: List<OptionalPluginDescriptor> = emptyList(),
  override val extensions: Map<String, List<Element>> = hashMapOf(),
  override val sinceBuild: IdeVersion = IdeVersion.createIdeVersion("IU-163.1"),
  override val untilBuild: IdeVersion? = null,
  override val definedModules: Set<String> = emptySet(),
  override val originalFile: Path? = null,
  override val appContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor(),
  override val projectContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor(),
  override val moduleContainerDescriptor: IdePluginContentDescriptor = MutableIdePluginContentDescriptor(),
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  override val modulesDescriptors: List<ModuleDescriptor> = emptyList(),
  @Deprecated("See IdePlugin::isV2")
  override val isV2: Boolean = false,
  override val hasPackagePrefix: Boolean = false,
  override val kotlinPluginMode: KotlinPluginMode = KotlinPluginMode.Implicit,
  override val classpath: Classpath = Classpath.EMPTY
) : IdePlugin {

  override val useIdeClassLoader = false
  override val isImplementationDetail = false
  override val hasDotNetPart: Boolean = false

  override val declaredThemes = emptyList<IdeTheme>()

  override fun isCompatibleWithIde(ideVersion: IdeVersion) =
    sinceBuild <= ideVersion && (untilBuild == null || ideVersion <= untilBuild)
}