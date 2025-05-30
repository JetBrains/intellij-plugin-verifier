/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorResolutionProblem
import java.nio.file.Path

internal class FileBasedModuleDescriptorResolver : AbstractModuleDescriptorResolver<FileBasedModule>() {
  override fun getModuleDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleDeclaration: FileBasedModule
  ): ModuleDescriptor {
    pluginCreator.plugin.addDependencies(module, moduleDeclaration.loadingRule)
    return ModuleDescriptor(
      moduleDeclaration.name,
      moduleDeclaration.loadingRule,
      module.dependencies,
      module,
      moduleDeclaration.configFile
    )
  }

  override fun getProblem(moduleReference: FileBasedModule, errors: List<PluginProblem>): PluginProblem {
    val (name, _, configFile) = moduleReference
    return ModuleDescriptorResolutionProblem(name, configFile, errors)
  }
}