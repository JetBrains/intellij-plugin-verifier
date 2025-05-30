/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.InlineDeclaredModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

internal abstract class AbstractModuleDescriptorResolver<M : Module> {

  protected abstract fun getModuleCreator(
    moduleReference: M,
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator

  internal fun resolve(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    moduleReference: M,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ) {
    val moduleCreator = getModuleCreator(moduleReference, pluginArtifactPath, pluginCreator, resourceResolver, problemResolver)
    val pluginCreationResult = moduleCreator.pluginCreationResult
    if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
      val module = pluginCreationResult.plugin

      val moduleDescriptor = getModuleDescriptor(pluginArtifactPath, pluginCreator, module, moduleCreator, moduleReference)
      pluginCreator.addModuleDescriptor(module, moduleDescriptor)
    } else {
      pluginCreator.registerProblem(getProblem(moduleReference, pluginCreationResult.errors))
    }
  }

  private fun PluginCreator.addModuleDescriptor(resolvedContentModule: IdePlugin, moduleDescriptor: ModuleDescriptor) {
    plugin.modulesDescriptors.add(moduleDescriptor)
    plugin.definedModules.add(moduleDescriptor.name)

    mergeContent(resolvedContentModule)
  }

  abstract fun getProblem(moduleReference: M, errors: List<PluginProblem>): PluginProblem

  abstract fun getModuleDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleReference: M
  ): ModuleDescriptor

  protected fun IdePluginImpl.addDependencies(module: IdePlugin, loadingRule: ModuleLoadingRule) {
    module.forEachDependencyNotIn(this) {
      val dependency = if (loadingRule.required) it else it.asOptional()
      dependencies += dependency
    }
  }

  protected fun IdePluginImpl.addInlineModuleDependencies(
    inlineModuleReference: InlineModule,
    module: IdePlugin,
    loadingRule: ModuleLoadingRule
  ) {
    module.forEachDependencyNotIn(this) {
      dependencies += InlineDeclaredModuleV2Dependency.of(it.id, loadingRule, contentModuleOwner = this, inlineModuleReference)
    }
  }

  protected fun IdePlugin.forEachDependencyNotIn(plugin: IdePlugin, dependencyHandler: (PluginDependency) -> Unit) {
    return dependencies
      .filter { dependency -> plugin.dependencies.none { it.id == dependency.id } }
      .forEach { dependencyHandler(it) }
  }

  protected val PluginCreationResult<IdePlugin>.errors: List<PluginProblem>
    get() = when (this) {
      is PluginCreationSuccess -> emptyList()
      is PluginCreationFail -> this.errorsAndWarnings.filter { it.level === ERROR }
    }

}