/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
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
    contentModulesOwner: PluginCreator,
    module: IdePlugin,
    moduleCreator: PluginCreator,
    moduleReference: InlineModule
  ): ModuleDescriptor {
    val resolvedDependencies = getDependencies(contentModulesOwner, module, moduleReference)
    return ModuleDescriptor.of(
      module,
      moduleReference,
      resolvedDependencies,
      getDeclaredDependencies(contentModulesOwner.plugin, module, moduleReference)
    )
  }

  override fun getModuleCreator(
    moduleReference: InlineModule,
    pluginArtifactPath: Path,
    contentModulesOwner: PluginCreator,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    val moduleDescriptorResource =
      getModuleDescriptorResource(moduleReference, pluginArtifactPath, contentModulesOwner.descriptorPath)
    return moduleLoader.loadPlugin(ModuleFromDescriptorLoader.Context(moduleReference.name, moduleDescriptorResource, contentModulesOwner, resourceResolver))
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
    contentModulesOwner: PluginCreator,
    module: IdePlugin,
    moduleReference: InlineModule
  ): List<PluginDependency> = getDependencies(contentModulesOwner.plugin, module, moduleReference)

  fun getDependencies(
    contentModulesOwner: IdePlugin,
    contentModule: IdePlugin,
    contentModuleReference: InlineModule
  ): List<PluginDependency> {
    return getDeclaredDependencies(contentModulesOwner, contentModule, contentModuleReference)
      .filter { moduleDep -> contentModulesOwner.dependencies.none { it.id == moduleDep.id } }
  }

  private fun getDeclaredDependencies(
    contentModulesOwner: IdePlugin,
    contentModule: IdePlugin,
    contentModuleReference: InlineModule
  ): List<PluginDependency> {
    return contentModule.mapDependency { it }.map {
        when (it) {
          is Dependency.PluginV1Dependency -> it.dependency.asPluginDependency()
          is Dependency.PluginV2Dependency ->
            InlineDeclaredModuleV2Dependency.onPlugin(
              it.id,
              contentModuleReference.loadingRule,
              contentModulesOwner,
              contentModuleReference
            )

          is Dependency.ModuleV2Dependency ->
            InlineDeclaredModuleV2Dependency.onModule(
              it.id,
              contentModuleReference.loadingRule,
              contentModulesOwner,
              contentModuleReference
            )
        }
      }
  }
}