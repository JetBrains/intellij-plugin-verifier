/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

private val LOG: Logger = LoggerFactory.getLogger(ModuleFromDescriptorLoader::class.java)

internal class ModuleFromDescriptorLoader : PluginLoader<ModuleFromDescriptorLoader.Context> {
  override fun loadPlugin(pluginLoadingContext: Context): PluginCreator = with(pluginLoadingContext) {
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

  internal data class Context(
    val moduleId: String,
    val descriptorResource: DescriptorResource,
    val parentPlugin: PluginCreator? = null,
    override val resourceResolver: ResourceResolver
  ) : PluginLoadingContext(
    resourceResolver,
    AnyProblemToWarningPluginCreationResultResolver,
  )
}