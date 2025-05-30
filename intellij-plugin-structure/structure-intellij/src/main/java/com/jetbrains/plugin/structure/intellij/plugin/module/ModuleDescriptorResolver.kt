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
import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

internal abstract class ModuleDescriptorResolver<M : Module> {

  sealed class ResolutionResult {
    data class Found(val resolvedContentModule: IdePlugin, val moduleDescriptor: ModuleDescriptor) : ResolutionResult()
    data class Failed(val error: PluginProblem) : ResolutionResult()
  }

  protected abstract fun getModuleCreator(
    moduleReference: M,
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator

  internal fun resolveDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    moduleReference: M,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): ResolutionResult {
    val moduleCreator = getModuleCreator(moduleReference, pluginArtifactPath, pluginCreator, resourceResolver, problemResolver)
    val pluginCreationResult = moduleCreator.pluginCreationResult
    return if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
      val resolvedContentModule = pluginCreationResult.plugin
      val moduleDescriptor = getModuleDescriptor(pluginArtifactPath, pluginCreator, resolvedContentModule, moduleCreator, moduleReference)
      ResolutionResult.Found(resolvedContentModule, moduleDescriptor)
    } else {
      ResolutionResult.Failed(getProblem(moduleReference, pluginCreationResult.errors))
    }
  }

  abstract fun getProblem(moduleReference: M, errors: List<PluginProblem>): PluginProblem

  abstract fun getModuleDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleReference: M
  ): ModuleDescriptor

  abstract fun getDependencies(moduleOwner: IdePluginImpl, module: IdePlugin, moduleReference: M): List<PluginDependency>

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