/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.InlineDeclaredModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(InlineModuleDescriptorResolver::class.java)

internal class InlineModuleDescriptorResolver : ModuleDescriptorResolver<InlineModule>() {

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
      moduleDescriptorResource
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
    return loadModuleFromDescriptorResource(
      moduleReference.name,
      moduleDescriptorResource,
      pluginCreator,
      resourceResolver
    )
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

  private fun loadModuleFromDescriptorResource(
    moduleId: String,
    descriptorResource: DescriptorResource,
    moduleOwner: PluginCreator? = null,
    resourceResolver: ResourceResolver
  ): PluginCreator {
    return descriptorResource.inputStream.use {
      try {
        val problemResolver = AnyProblemToWarningPluginCreationResultResolver
        val descriptorXml = JDOMUtil.loadDocument(it)
        createPlugin(
          descriptorResource,
          moduleOwner,
          descriptorXml,
          resourceResolver,
          problemResolver
        ).also { creator ->
          logPluginCreationWarnings(moduleId, creator)
        }
      } catch (e: IOException) {
        with(descriptorResource) {
          LOG.warn("Unable to read descriptor stream (source: '$uri')", e)
          val problem = UnableToReadDescriptor(fileName, e.localizedMessage)
          createInvalidPlugin(artifactFileName, fileName, problem)
        }
      }
    }
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

  private fun logPluginCreationWarnings(pluginId: String, pluginCreator: PluginCreator) {
    val pluginCreationResult = pluginCreator.pluginCreationResult
    if (LOG.isDebugEnabled && pluginCreationResult is PluginCreationSuccess) {
      val warningMessage = pluginCreationResult.warnings.joinToString("\n") {
        it.message
      }
      LOG.debug("Plugin or module '$pluginId' has plugin problems: $warningMessage")
    }
  }
}