/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.loaders.JarOrDirectoryPluginLoader
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorResolutionProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

internal class FileBasedModuleDescriptorResolver(private val pluginLoader: JarOrDirectoryPluginLoader) :
  ModuleDescriptorResolver<FileBasedModule>() {

  override fun getModuleDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleReference: FileBasedModule
  ): ModuleDescriptor {
    pluginCreator.plugin.dependencies += getDependencies(pluginCreator.plugin, module, moduleReference)
    return ModuleDescriptor(
      moduleReference.name,
      moduleReference.loadingRule,
      module,
      moduleReference.configFile,
      moduleReference
    )
  }

  override fun getModuleCreator(
    moduleReference: FileBasedModule,
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    val moduleJarFile = pluginArtifactPath.resolve("lib/modules/${moduleReference.name}.jar")
    val modulePath = if (moduleJarFile.isFile) moduleJarFile else pluginArtifactPath
    return pluginLoader.loadPlugin(JarOrDirectoryPluginLoader.Context(
      modulePath,
      moduleReference.configFile,
      false,
      resourceResolver,
      pluginCreator,
      problemResolver
    ))
  }

  override fun getProblem(moduleReference: FileBasedModule, errors: List<PluginProblem>): PluginProblem {
    return ModuleDescriptorResolutionProblem(moduleReference.name, moduleReference.configFile, errors)
  }

  override fun getDependencies(
    moduleOwner: IdePluginImpl,
    module: IdePlugin,
    moduleReference: FileBasedModule
  ): List<PluginDependency> {
    return mutableListOf<PluginDependency>().also { dependencies ->
      module.forEachDependencyNotIn(moduleOwner) {
        dependencies += if (moduleReference.loadingRule.required) it else it.asOptional()
      }
    }
  }
}