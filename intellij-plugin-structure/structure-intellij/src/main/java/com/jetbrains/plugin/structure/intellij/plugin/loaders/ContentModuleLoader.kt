/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ContentModuleLoader::class.java)

class ContentModuleLoader internal constructor(pluginLoader: PluginLoader) {
  private val fileBasedModuleDescriptorResolver = FileBasedModuleDescriptorResolver(pluginLoader)
  private val inlineModuleDescriptorResolver = InlineModuleDescriptorResolver()

  internal fun resolveContentModules(
    pluginFile: Path,
    currentPlugin: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ) {
    if (currentPlugin.isSuccess) {
      val contentModules = currentPlugin.plugin.contentModules
      contentModules.map { module ->
        when (module) {
          is FileBasedModule -> resolveFileBasedModule(
            module,
            pluginFile,
            currentPlugin,
            resourceResolver,
            problemResolver
          )

          is InlineModule -> resolveInlineModule(module, pluginFile, currentPlugin, resourceResolver)
        }
      }.map { resolution ->
        when (resolution) {
          is Found -> currentPlugin.addModuleDescriptor(resolution.resolvedContentModule, resolution.moduleDescriptor)
          is Failed -> currentPlugin.registerProblem(resolution.error)
        }
      }
    }
  }

  private fun PluginCreator.addModuleDescriptor(resolvedContentModule: IdePlugin, moduleDescriptor: ModuleDescriptor) {
    plugin.modulesDescriptors.add(moduleDescriptor)
    plugin.definedModules.add(moduleDescriptor.name)

    mergeContent(resolvedContentModule)
  }

  private fun resolveFileBasedModule(
    module: FileBasedModule,
    pluginFile: Path,
    currentPlugin: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): ResolutionResult {
    return fileBasedModuleDescriptorResolver.resolveDescriptor(
      pluginFile,
      currentPlugin,
      module,
      resourceResolver,
      problemResolver
    )
  }

  private fun resolveInlineModule(
    module: InlineModule,
    pluginFile: Path,
    currentPlugin: PluginCreator,
    resourceResolver: ResourceResolver
  ): ResolutionResult {
    return inlineModuleDescriptorResolver.resolveDescriptor(
      pluginFile,
      currentPlugin,
      module,
      resourceResolver,
      AnyProblemToWarningPluginCreationResultResolver
    )
  }
}