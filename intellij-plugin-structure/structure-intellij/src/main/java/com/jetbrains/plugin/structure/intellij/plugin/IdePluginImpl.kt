/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.KotlinPluginMode.Implicit
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.nio.file.Path

class IdePluginImpl : IdePlugin, StructurallyValidated {
  override var pluginId: String? = null

  override var pluginName: String? = null

  override var pluginVersion: String? = null

  override var sinceBuild: IdeVersion? = null

  override var untilBuild: IdeVersion? = null

  override var originalFile: Path? = null

  override var productDescriptor: ProductDescriptor? = null

  override var vendor: String? = null

  override var vendorEmail: String? = null

  override var vendorUrl: String? = null

  override var description: String? = null

  override var changeNotes: String? = null

  override var url: String? = null

  override var useIdeClassLoader: Boolean = false

  override var classpath: Classpath = Classpath.EMPTY

  override var isImplementationDetail: Boolean = false

  @Deprecated("See IdePlugin::isV2")
  override val isV2: Boolean
    get() = hasPackagePrefix

  override var hasPackagePrefix: Boolean = false
    
  override var kotlinPluginMode: KotlinPluginMode = Implicit

  override var hasDotNetPart: Boolean = false

  override var underlyingDocument: Document = Document()

  override val declaredThemes: MutableList<IdeTheme> = arrayListOf()

  /**
   * Plugin aliases mapped from the `idea-plugin/module` element.
   */
  override val definedModules: MutableSet<String> = hashSetOf()

  override val dependencies: MutableList<PluginDependency> = arrayListOf()

  override val incompatibleWith: MutableList<String> = arrayListOf()

  override val extensions: MutableMap<String, MutableList<Element>> = HashMap()

  val actions: MutableList<Element> = arrayListOf()

  override val appContainerDescriptor = MutableIdePluginContentDescriptor()

  override val projectContainerDescriptor = MutableIdePluginContentDescriptor()

  override val moduleContainerDescriptor = MutableIdePluginContentDescriptor()

  override var icons: List<PluginIcon> = emptyList()

  override val optionalDescriptors: MutableList<OptionalPluginDescriptor> = arrayListOf()

  override val modulesDescriptors: MutableList<ModuleDescriptor> = arrayListOf()

  override val contentModules: MutableList<Module> = arrayListOf()

  override var thirdPartyDependencies: List<ThirdPartyDependency> = emptyList()

  override fun isCompatibleWithIde(ideVersion: IdeVersion) =
    (sinceBuild == null || sinceBuild!! <= ideVersion) && (untilBuild == null || ideVersion <= untilBuild!!)

  override val problems: MutableList<PluginProblem> = mutableListOf()

  override fun toString(): String =
    (pluginId ?: pluginName ?: "<unknown plugin ID>") + (pluginVersion?.let { ":$it" } ?: "")

  companion object {
    /**
     * Clones an existing plugin into a new `IdePlugin` instance.
     *
     * If the [old][IdePlugin] instance is [StructurallyValidated], the
     * provided list of plugin problems will replace the original list of plugin problems in the [problems] field.
     *
     * @param old the plugin that will be cloned.
     * @param overriddenProblems a list of plugin problems that will be explicitly assigned to the cloned instance
     * if the [old] instance is [StructurallyValidated].
     */
    fun clone(old: IdePlugin, overriddenProblems: List<PluginProblem>): IdePlugin {
      return IdePluginImpl().apply {
        pluginId = old.pluginId
        pluginName = old.pluginName
        pluginVersion = old.pluginVersion
        sinceBuild = old.sinceBuild
        untilBuild = old.untilBuild
        originalFile = old.originalFile
        productDescriptor = old.productDescriptor
        vendor = old.vendor
        vendorEmail = old.vendorEmail
        vendorUrl = old.vendorUrl
        description = old.description
        changeNotes = old.changeNotes
        url = old.url
        useIdeClassLoader = old.useIdeClassLoader
        classpath = old.classpath
        isImplementationDetail = old.isImplementationDetail
        hasPackagePrefix = old.hasPackagePrefix
        kotlinPluginMode = old.kotlinPluginMode
        hasDotNetPart = old.hasDotNetPart
        underlyingDocument = old.underlyingDocument
        declaredThemes.addAll(old.declaredThemes)
        definedModules.addAll(old.definedModules)
        dependencies.addAll(old.dependencies)
        incompatibleWith.addAll(old.incompatibleWith)
        if (old is IdePluginImpl) {
          extensions.putAll(old.extensions)
          actions.addAll(old.actions)
          old.appContainerDescriptor.copyInto(appContainerDescriptor)
          old.projectContainerDescriptor.copyInto(projectContainerDescriptor)
          old.moduleContainerDescriptor.copyInto(moduleContainerDescriptor)
        }
        icons = old.icons.toMutableList()
        optionalDescriptors.addAll(old.optionalDescriptors)
        modulesDescriptors.addAll(old.modulesDescriptors)
        contentModules.addAll(old.contentModules)
        thirdPartyDependencies = old.thirdPartyDependencies.toMutableList()
        if (old is StructurallyValidated) {
          problems.addAll(overriddenProblems)
        }
      }
    }

    private fun IdePluginContentDescriptor.copyInto(descriptor: MutableIdePluginContentDescriptor) {
      descriptor.services.addAll(services)
      descriptor.components.addAll(components)
      descriptor.listeners.addAll(listeners)
      descriptor.extensionPoints.addAll(extensionPoints)
    }
  }
}
