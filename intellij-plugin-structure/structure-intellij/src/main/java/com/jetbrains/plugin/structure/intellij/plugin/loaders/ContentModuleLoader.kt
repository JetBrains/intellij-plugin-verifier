/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginLoader
import com.jetbrains.plugin.structure.intellij.plugin.module.AbstractModuleDescriptorResolver.ResolutionResult
import com.jetbrains.plugin.structure.intellij.plugin.module.AbstractModuleDescriptorResolver.ResolutionResult.Failed
import com.jetbrains.plugin.structure.intellij.plugin.module.AbstractModuleDescriptorResolver.ResolutionResult.Found
import com.jetbrains.plugin.structure.intellij.plugin.module.FileBasedModuleDescriptorResolver
import com.jetbrains.plugin.structure.intellij.plugin.module.InlineModuleDescriptorResolver
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

class ContentModuleLoader internal constructor(pluginLoader: PluginLoader) {
  private val fileBasedModuleDescriptorResolver = FileBasedModuleDescriptorResolver(pluginLoader)
  private val inlineModuleDescriptorResolver = InlineModuleDescriptorResolver()

  internal fun resolveContentModules(
    pluginFile: Path,
    contentModulesOwner: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ) {
    if (contentModulesOwner.isSuccess) {
      val contentModules = contentModulesOwner.plugin.contentModules
      contentModules
        .map { resolveContentModule(it, pluginFile, contentModulesOwner, resourceResolver, problemResolver) }
        .map {
          when (it) {
            is Found -> contentModulesOwner.addModuleDescriptor(it.resolvedContentModule, it.moduleDescriptor)
            is Failed -> contentModulesOwner.registerProblem(it.error)
          }
        }
    }
  }

  private fun resolveContentModule(
    moduleReference: Module,
    pluginFile: Path,
    contentModulesOwner: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): ResolutionResult {
    return when (moduleReference) {
      is FileBasedModule -> fileBasedModuleDescriptorResolver.resolveDescriptor(
        pluginFile,
        contentModulesOwner,
        moduleReference,
        resourceResolver,
        problemResolver
      )

      is InlineModule -> inlineModuleDescriptorResolver.resolveDescriptor(
        pluginFile,
        contentModulesOwner,
        moduleReference,
        resourceResolver,
        AnyProblemToWarningPluginCreationResultResolver
      )
    }
  }

  private fun PluginCreator.addModuleDescriptor(resolvedContentModule: IdePlugin, moduleDescriptor: ModuleDescriptor) {
    plugin.modulesDescriptors.add(moduleDescriptor)
    plugin.definedModules.add(moduleDescriptor.name)

    mergeContent(resolvedContentModule)
  }
}