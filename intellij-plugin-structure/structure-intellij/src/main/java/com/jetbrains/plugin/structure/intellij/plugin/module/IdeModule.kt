package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.Classpath.Companion.EMPTY
import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.plugin.KotlinPluginMode
import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.ContentModuleDependency
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.MutableIdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.OptionalPluginDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginMainModuleDependency
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element

typealias Dependency = ModuleBean.ModuleDependency
typealias Resource = ModuleBean.ResourceRoot

class IdeModule(override val pluginId: String, override val classpath: Classpath = EMPTY,
                override val hasPackagePrefix: Boolean) : IdePlugin {
  val moduleDependencies = mutableListOf<Dependency>()
  val resources = mutableListOf<Resource>()
  override var underlyingDocument = Document()

  override val extensions = mutableMapOf<String, List<Element>>()
  override val appContainerDescriptor = MutableIdePluginContentDescriptor()
  override val projectContainerDescriptor = MutableIdePluginContentDescriptor()
  override val moduleContainerDescriptor = MutableIdePluginContentDescriptor()

  private val _dependsList = mutableListOf<DependsPluginDependency>()
  override val dependsList: List<DependsPluginDependency> get() = _dependsList

  fun addDepends(depends: DependsPluginDependency) = _dependsList.add(depends)

  private val _contentModuleDependencies = mutableListOf<ContentModuleDependency>()
  override val contentModuleDependencies: List<ContentModuleDependency> get() = _contentModuleDependencies

  fun addContentModuleDependency(moduleDependency: ContentModuleDependency) = _contentModuleDependencies.add(moduleDependency)

  private val _pluginMainModuleDependencies = mutableListOf<PluginMainModuleDependency>()
  override val pluginMainModuleDependencies: List<PluginMainModuleDependency> get() = _pluginMainModuleDependencies

  fun addPluginMainModuleDependency(pluginMainModuleDependency: PluginMainModuleDependency) = _pluginMainModuleDependencies.add(pluginMainModuleDependency)

  override val contentModules: List<Module> = emptyList()
  override val dependencies = mutableListOf<PluginDependency>()

  private val _pluginAliases: MutableSet<String> = mutableSetOf()
  override val pluginAliases: Set<String> get() = _pluginAliases

  private val _definedModules = mutableSetOf<String>()

  @Deprecated("use either pluginAliases or contentModules")
  override val definedModules: Set<String> get() = _definedModules + pluginAliases + setOf(pluginId)

  override val optionalDescriptors = emptyList<OptionalPluginDescriptor>()
  override val modulesDescriptors = emptyList<ModuleDescriptor>()

  override val pluginName = null
  override val pluginVersion = null
  override val originalFile = null
  override val productDescriptor = null
  override val useIdeClassLoader = false
  @Deprecated("See IdePlugin::isV2")
  override val isV2 = true
  override val kotlinPluginMode: KotlinPluginMode = KotlinPluginMode.Implicit
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
  override val incompatibleWith = emptyList<String>()
  override val isImplementationDetail = false
  override val hasDotNetPart = false
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList()

  override fun isCompatibleWithIde(ideVersion: IdeVersion) = true

  companion object {
    @Throws(IllegalArgumentException::class)
    fun clone(plugin: IdePlugin, pluginId: String, classpath: Classpath): IdeModule {
      return IdeModule(pluginId,classpath, hasPackagePrefix = plugin.hasPackagePrefix).apply {
        underlyingDocument = plugin.underlyingDocument.clone()

        extensions.putAll(plugin.extensions)
        plugin.dependsList.forEach { addDepends(it) }
        plugin.contentModuleDependencies.forEach { addContentModuleDependency(it) }
        plugin.pluginMainModuleDependencies.forEach { addPluginMainModuleDependency(it) }
        dependencies += plugin.dependencies
        _pluginAliases.addAll(plugin.pluginAliases)
        _definedModules.addAll(plugin.definedModules)

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

