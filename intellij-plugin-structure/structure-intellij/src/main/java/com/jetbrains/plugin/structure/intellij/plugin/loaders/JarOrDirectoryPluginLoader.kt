/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.problems.IncorrectJarOrDirectory
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(JarOrDirectoryPluginLoader::class.java)

internal class JarOrDirectoryPluginLoader(private val pluginLoaderRegistry: PluginLoaderProvider) : PluginLoader<JarOrDirectoryPluginLoader.Context> {
  private val jarLoader: JarPluginLoader
    get() = pluginLoaderRegistry.get<JarPluginLoader.Context, JarPluginLoader>()

  private val dirLoader: PluginDirectoryLoader
    get() = pluginLoaderRegistry.get<PluginDirectoryLoader.Context, PluginDirectoryLoader>()

  override fun loadPlugin(pluginLoadingContext: Context): PluginCreator = with(pluginLoadingContext) {
    LOG.debug("Loading {} with descriptor [{}]", jarOrDirectory, descriptorPath)
    val systemIndependentDescriptorPath = descriptorPath.toSystemIndependentName()
    return when {
      jarOrDirectory.isDirectory -> {
        dirLoader.loadPlugin(PluginDirectoryLoader.Context(jarOrDirectory,
          systemIndependentDescriptorPath,
          validateDescriptor,
          resourceResolver,
          parentPlugin,
          problemResolver))
      }

      jarOrDirectory.isJar() -> jarLoader.loadPlugin(
        JarPluginLoader.Context(
          jarOrDirectory,
          systemIndependentDescriptorPath,
          validateDescriptor,
          resourceResolver,
          parentPlugin,
          problemResolver
        )
      )

      else -> createInvalidPlugin()
    }
  }

  private fun Context.createInvalidPlugin() =
    createInvalidPlugin(jarOrDirectory, descriptorPath, IncorrectJarOrDirectory(jarOrDirectory))

  internal data class Context(
    val jarOrDirectory: Path,
    val descriptorPath: String,
    val validateDescriptor: Boolean,
    override val resourceResolver: ResourceResolver,
    val parentPlugin: PluginCreator?,
    override val problemResolver: PluginCreationResultResolver,
    val hasDotNetDirectory: Boolean = false
  ) : PluginLoadingContext(
    resourceResolver,
    problemResolver,
  )
}