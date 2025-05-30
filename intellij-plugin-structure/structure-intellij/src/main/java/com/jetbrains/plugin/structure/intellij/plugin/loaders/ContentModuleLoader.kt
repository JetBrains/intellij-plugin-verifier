/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginLoader
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ContentModuleLoader::class.java)

class ContentModuleLoader internal constructor(private val pluginLoader: PluginLoader) {
  internal fun resolveContentModules(pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver, problemResolver: PluginCreationResultResolver) {
    if (currentPlugin.isSuccess) {
      val contentModules = currentPlugin.plugin.contentModules
      for (module in contentModules) {
        when (module) {
          is FileBasedModule -> resolveFileBasedModule(
            module,
            pluginFile,
            currentPlugin,
            resourceResolver,
            problemResolver
          )

          is InlineModule -> resolveInlineModule(module, pluginFile, currentPlugin, resourceResolver)
        }
      }
    }
  }

  private fun resolveFileBasedModule(module: FileBasedModule, pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver, problemResolver: PluginCreationResultResolver) {
    val configFile = module.configFile
    val moduleCreator = pluginLoader.load(
      pluginFile,
      configFile,
      false,
      resourceResolver,
      currentPlugin,
      problemResolver
    )
    currentPlugin.addModuleDescriptor(module.name, module.loadingRule, configFile, moduleCreator)
  }

  private fun resolveInlineModule(module: InlineModule, pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver) {
    val moduleDescriptorResource = getDescriptorResource(module, pluginFile, currentPlugin.descriptorPath)
    val moduleCreator =
      loadModuleFromDescriptorResource(module.name, moduleDescriptorResource, currentPlugin, resourceResolver)
    currentPlugin.addModuleDescriptor(module, module.loadingRule, moduleDescriptorResource, moduleCreator)
  }

  private fun getDescriptorResource(module: InlineModule, pluginFile: Path, descriptorPath: String): DescriptorResource {
    // TODO descriptor path is not relative to the pluginFile JAR. See MP-7224
    val parentUriStr = if (pluginFile.isJar()) {
      "jar:" + pluginFile.toUri().toString() + "!" + descriptorPath.toSystemIndependentName()
    } else {
      pluginFile.toUri().toString() + "/" + descriptorPath.toSystemIndependentName()
    }
    val uriStr = parentUriStr + "#modules/" + module.name
    return DescriptorResource(module.textContent.byteInputStream(), URI(uriStr), URI(parentUriStr))
  }

  private fun loadModuleFromDescriptorResource(
    moduleId: String,
    descriptorResource: DescriptorResource,
    parentPlugin: PluginCreator? = null,
    resourceResolver: ResourceResolver
  ): PluginCreator {
    return descriptorResource.inputStream.use {
      try {
        val problemResolver = AnyProblemToWarningPluginCreationResultResolver
        val descriptorXml = JDOMUtil.loadDocument(it)
        createPlugin(
          descriptorResource,
          parentPlugin,
          descriptorXml,
          resourceResolver,
          problemResolver
        ).also {
          logPluginCreationWarnings(moduleId, it)
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