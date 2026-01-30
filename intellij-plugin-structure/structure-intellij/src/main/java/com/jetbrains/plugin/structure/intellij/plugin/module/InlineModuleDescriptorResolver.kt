/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.InlineDeclaredModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.plugin.loaders.ModuleFromDescriptorLoader
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.net.URI
import java.nio.file.Path

internal class InlineModuleDescriptorResolver(private val moduleLoader: ModuleFromDescriptorLoader) : ModuleDescriptorResolver<InlineModule>() {

  override fun getModuleDescriptor(
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleReference: InlineModule
  ): ModuleDescriptor {
    pluginCreator.plugin.dependencies += getDependencies(pluginCreator.plugin, module, moduleReference)
    val moduleDescriptorResource =
      getModuleDescriptorResource(moduleReference, pluginArtifactPath, pluginCreator.descriptorPath)
    return ModuleDescriptor.of(
      moduleReference.name,
      moduleReference.loadingRule,
      module,
      moduleDescriptorResource,
      moduleReference
    )
  }

  override fun getModuleCreator(
    moduleReference: InlineModule,
    pluginArtifactPath: Path,
    pluginCreator: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    val moduleDescriptorResource =
      getModuleDescriptorResource(moduleReference, pluginArtifactPath, pluginCreator.descriptorPath)
    return moduleLoader.loadPlugin(ModuleFromDescriptorLoader.Context(moduleReference.name, moduleDescriptorResource, pluginCreator, resourceResolver))
  }

  override fun getProblem(
    moduleReference: InlineModule,
    errors: List<PluginProblem>
  ): PluginProblem {
    return ModuleDescriptorProblem(moduleReference, errors)
  }

  private fun getModuleDescriptorResource(
    module: InlineModule,
    moduleOwnerPath: Path,
    descriptorPath: String
  ): DescriptorResource {
    // TODO descriptor path is not relative to the pluginFile JAR. See MP-7224
    val parentUriStr = if (moduleOwnerPath.isJar()) {
      "jar:" + moduleOwnerPath.toUri().toString() + "!" + descriptorPath.toSystemIndependentName()
    } else {
      moduleOwnerPath.toUri().toString() + "/" + descriptorPath.toSystemIndependentName()
    }
    val uriStr = parentUriStr + "#modules/" + module.name
    return DescriptorResource(module.textContent.byteInputStream(), URI(uriStr), URI(parentUriStr))
  }

  override fun getDependencies(
    moduleOwner: IdePluginImpl,
    module: IdePlugin,
    moduleReference: InlineModule
  ): MutableList<PluginDependency> {
    return mutableListOf<PluginDependency>().also { dependencies ->
      module.forEachDependencyNotIn(moduleOwner) {
        dependencies += when (it) {
          is PluginV2Dependency -> InlineDeclaredModuleV2Dependency.onPlugin(
            it.id,
            moduleReference.loadingRule,
            moduleOwner,
            moduleReference)
          is ModuleV2Dependency -> InlineDeclaredModuleV2Dependency.onModule(
            it.id,
            moduleReference.loadingRule,
            moduleOwner,
            moduleReference)
          else -> it
        }
      }
    }
  }
}