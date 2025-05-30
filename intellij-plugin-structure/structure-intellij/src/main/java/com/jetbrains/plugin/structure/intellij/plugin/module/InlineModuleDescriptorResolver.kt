/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorProblem
import java.net.URI
import java.nio.file.Path

internal class InlineModuleDescriptorResolver  : AbstractModuleDescriptorResolver<InlineModule>() {

  override fun getModuleDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleDeclaration: InlineModule
  ): ModuleDescriptor {
    val moduleName = moduleDeclaration.name
    val inlineModule = module
    pluginCreator.plugin.addInlineModuleDependencies(moduleDeclaration, inlineModule, moduleDeclaration.loadingRule)
    val moduleDescriptorResource = getModuleDescriptorResource(moduleDeclaration, pluginArtifactPath, pluginCreator.descriptorPath)
    return ModuleDescriptor.of(
      moduleName,
      moduleDeclaration.loadingRule,
      inlineModule,
      moduleDescriptorResource
    )
  }

  override fun getProblem(
    moduleReference: InlineModule,
    errors: List<PluginProblem>
  ): PluginProblem {
    return ModuleDescriptorProblem(moduleReference, errors)
  }

  private fun getModuleDescriptorResource(module: InlineModule, pluginFile: Path, descriptorPath: String): DescriptorResource {
    // TODO descriptor path is not relative to the pluginFile JAR. See MP-7224
    val parentUriStr = if (pluginFile.isJar()) {
      "jar:" + pluginFile.toUri().toString() + "!" + descriptorPath.toSystemIndependentName()
    } else {
      pluginFile.toUri().toString() + "/" + descriptorPath.toSystemIndependentName()
    }
    val uriStr = parentUriStr + "#modules/" + module.name
    return DescriptorResource(module.textContent.byteInputStream(), URI(uriStr), URI(parentUriStr))
  }
}