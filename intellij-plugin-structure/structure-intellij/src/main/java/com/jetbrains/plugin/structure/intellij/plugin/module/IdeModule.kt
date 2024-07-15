package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.MutableIdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.OptionalPluginDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

typealias Dependency = ModuleBean.ModuleDependency
typealias Resource = ModuleBean.ResourceRoot

class IdeModule(override val pluginId: String) : IdePlugin {
  val classpath = mutableListOf<Path>()
  val moduleDependencies = mutableListOf<Dependency>()
  val resources = mutableListOf<Resource>()
  override var underlyingDocument = Document()

  override val extensions = mutableMapOf<String, List<Element>>()
  override val appContainerDescriptor = MutableIdePluginContentDescriptor()
  override val projectContainerDescriptor = MutableIdePluginContentDescriptor()
  override val moduleContainerDescriptor = MutableIdePluginContentDescriptor()
  override val dependencies = mutableListOf<PluginDependency>()
  override val definedModules = mutableSetOf<String>()
  override val optionalDescriptors = emptyList<OptionalPluginDescriptor>()
  override val modulesDescriptors = emptyList<ModuleDescriptor>()

  override val pluginName = null
  override val pluginVersion = null
  override val originalFile = null
  override val productDescriptor = null
  override val useIdeClassLoader = false
  override val isV2 = true
  override val url = null
  override val changeNotes = null
  override val description = null
  override val vendor = null
  override val vendorEmail = null
  override val vendorUrl = null

  override val sinceBuild = null
  override val untilBuild = null
  override val icons = emptyList<PluginIcon>()
  override val declaredThemes = emptyList<IdeTheme>()
  override val incompatibleModules = emptyList<String>()
  override val isImplementationDetail = false
  override val hasDotNetPart = false
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList()

  override fun isCompatibleWithIde(ideVersion: IdeVersion) = true

  companion object {
    @Throws(IllegalArgumentException::class)
    fun clone(plugin: IdePlugin, pluginId: String): IdeModule {
      return IdeModule(pluginId).apply {
        underlyingDocument = plugin.underlyingDocument.clone()

        extensions.putAll(plugin.extensions)
        dependencies += plugin.dependencies
        definedModules += plugin.definedModules

        appContainerDescriptor.copyFrom(plugin.appContainerDescriptor)
        projectContainerDescriptor.copyFrom(plugin.projectContainerDescriptor)
        moduleContainerDescriptor.copyFrom(plugin.moduleContainerDescriptor)
      }
    }
  }
}

private fun MutableIdePluginContentDescriptor.copyFrom(descriptor: IdePluginContentDescriptor) {
  services.addAll(descriptor.services)
  components.addAll(descriptor.components)
  listeners.addAll(descriptor.listeners)
  extensionPoints.addAll(descriptor.extensionPoints)
}

