/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginLoader
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorResolutionProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

internal class FileBasedModuleDescriptorResolver(private val pluginLoader: PluginLoader) :
  ModuleDescriptorResolver<FileBasedModule>() {

  override fun getModuleDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleReference: FileBasedModule
  ): ModuleDescriptor {
    addDependencies(pluginCreator.plugin, module, moduleReference)
    return ModuleDescriptor(
      moduleReference.name,
      moduleReference.loadingRule,
      module.dependencies,
      module,
      moduleReference.configFile
    )
  }

  override fun getModuleCreator(
    moduleReference: FileBasedModule,
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    return pluginLoader.load(
      pluginArtifactPath,
      moduleReference.configFile,
      false,
      resourceResolver,
      pluginCreator,
      problemResolver
    )
  }

  override fun getProblem(moduleReference: FileBasedModule, errors: List<PluginProblem>): PluginProblem {
    val (name, _, configFile) = moduleReference
    return ModuleDescriptorResolutionProblem(name, configFile, errors)
  }

  override fun addDependencies(
    moduleOwner: IdePluginImpl,
    module: IdePlugin,
    moduleReference: FileBasedModule
  ) {
    module.forEachDependencyNotIn(moduleOwner) {
      val dependency = if (moduleReference.loadingRule.required) it else it.asOptional()
      moduleOwner.dependencies += dependency
    }
  }
}