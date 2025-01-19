/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource

data class ModuleDescriptor(
  val name: String,
  val dependencies: List<PluginDependency>,
  val module: IdePlugin,
  val configurationFilePath: String
) {
  companion object {
    fun of(moduleId: String, module: IdePlugin, moduleDescriptorResource: DescriptorResource): ModuleDescriptor =
      ModuleDescriptor(moduleId, module.dependencies, module, moduleDescriptorResource.uri.toString())
  }
}