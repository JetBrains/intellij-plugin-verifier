/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.base.utils.withPathSeparatorOf
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.META_INF
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.jdom2.input.JDOMParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(PluginDirectoryLoader::class.java)

internal class PluginDirectoryLoader(private val pluginLoaderRegistry: PluginLoaderProvider) : PluginLoader<PluginDirectoryLoader.Context> {
  private val libDirectoryLoader: LibDirectoryPluginLoader
    get() = pluginLoaderRegistry.get<LibDirectoryPluginLoader.Context, LibDirectoryPluginLoader>()

  private val pluginIconLoader = PluginIconLoader()

  private val thirdPartyDependencyLoader = ThirdPartyDependencyLoader()

  override fun loadPlugin(pluginLoadingContext: Context): PluginCreator = with(pluginLoadingContext) {
    val descriptorFile = pluginDirectory.resolve(META_INF).resolve(descriptorPath.withPathSeparatorOf(pluginDirectory))
    return if (!descriptorFile.exists()) {
      libDirectoryLoader
        .loadPlugin(
          LibDirectoryPluginLoader.Context(
            pluginDirectory,
            descriptorPath,
            validateDescriptor,
            resourceResolver,
            parentPlugin,
            problemResolver
          )
        )
    } else try {
      val document = JDOMUtil.loadDocument(Files.newInputStream(descriptorFile))
      val icons = pluginIconLoader.load(pluginDirectory)
      val dependencies = thirdPartyDependencyLoader.load(pluginDirectory)
      createPlugin(
        pluginDirectory.simpleName, descriptorPath, parentPlugin,
        validateDescriptor, document, descriptorFile,
        resourceResolver, problemResolver
      ).apply {
        setIcons(icons)
        setThirdPartyDependencies(dependencies)
        setHasDotNetPart(hasDotNetDirectory)
      }
    } catch (e: JDOMParseException) {
      LOG.info("Unable to parse plugin descriptor $descriptorPath of plugin $descriptorFile", e)
      createInvalidPlugin(pluginDirectory, descriptorPath, UnexpectedDescriptorElements(e.lineNumber, descriptorPath))
    } catch (e: Exception) {
      LOG.info("Unable to read plugin descriptor $descriptorPath of plugin $descriptorFile", e)
      createInvalidPlugin(pluginDirectory, descriptorPath, UnableToReadDescriptor(descriptorPath, descriptorPath))
    }
  }

  internal data class Context(
    val pluginDirectory: Path,
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