/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource

data class ModuleDescriptor(
  val module: IdePlugin,
  val configurationFilePath: String,
  val moduleDefinition: Module
) {
  val name = moduleDefinition.name

  companion object {
    fun of(
      module: IdePlugin,
      moduleDescriptorResource: DescriptorResource,
      moduleDefinition: Module
    ): ModuleDescriptor =
      ModuleDescriptor(module, moduleDescriptorResource.uri.toString(), moduleDefinition)
  }
}

val ModuleDescriptor.dependencies: List<PluginDependency> get() = module.dependencies