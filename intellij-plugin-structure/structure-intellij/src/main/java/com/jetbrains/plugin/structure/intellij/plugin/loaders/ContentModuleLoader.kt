/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginLoader
import com.jetbrains.plugin.structure.intellij.plugin.module.ContentModuleLoadingResults
import com.jetbrains.plugin.structure.intellij.plugin.module.FileBasedModuleDescriptorResolver
import com.jetbrains.plugin.structure.intellij.plugin.module.InlineModuleDescriptorResolver
import com.jetbrains.plugin.structure.intellij.plugin.module.ModuleDescriptorResolver.ResolutionResult
import com.jetbrains.plugin.structure.intellij.plugin.module.ModuleDescriptorResolver.ResolutionResult.Failed
import com.jetbrains.plugin.structure.intellij.plugin.module.ModuleDescriptorResolver.ResolutionResult.Found
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
  ): ContentModuleLoadingResults {
    val loadingResults = ContentModuleLoadingResults()
    if (contentModulesOwner.isSuccess) {
      contentModulesOwner.plugin.contentModules
        .map { resolveContentModule(it, pluginFile, contentModulesOwner, resourceResolver, problemResolver) }
        .map {
          when (it) {
            is Found -> loadingResults.add(it.resolvedContentModule, it.moduleDescriptor)
            is Failed -> loadingResults.registerProblem(it.error)
          }
        }
    }
    return loadingResults
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
}