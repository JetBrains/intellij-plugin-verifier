/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.getShortExceptionMessage
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.base.zip.newZipHandler
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.META_INF
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.jar.JarArchiveCannotBeOpenException
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import com.jetbrains.plugin.structure.jar.PluginJar
import org.apache.commons.io.FilenameUtils
import org.jdom2.input.JDOMParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(JarPluginLoader::class.java)

internal class JarPluginLoader(private val fileSystemProvider: JarFileSystemProvider) : PluginLoader<JarPluginLoader.Context> {
  override fun loadPlugin(pluginLoadingContext: Context): PluginCreator = with(pluginLoadingContext) {
    return try {
      PluginJar(jarPath, fileSystemProvider).use { jar ->
        when (val descriptor = jar.getPluginDescriptor("$META_INF/$descriptorPath")) {
          is Found -> {
            try {
              val descriptorXml = descriptor.loadXml()
              createPlugin(jarPath.simpleName, descriptorPath, parentPlugin, validateDescriptor, descriptorXml, descriptor.path, resourceResolver, problemResolver).apply {
                setIcons(jar.getIcons())
                setThirdPartyDependencies(jar.getThirdPartyDependencies())
                setHasDotNetPart(hasDotNetDirectory)
              }
            } catch (e: JDOMParseException) {
              val message = e.localizedMessage
              LOG.warn("Unable to read descriptor [$descriptorPath] from [$jarPath]: $message")
              createInvalidPlugin(jarPath, descriptorPath, UnableToReadDescriptor(descriptorPath, message))
            } catch (e: Exception) {
              LOG.warn("Unable to read descriptor [$descriptorPath] from [$jarPath]", e)
              val message = e.localizedMessage
              createInvalidPlugin(jarPath, descriptorPath, UnableToReadDescriptor(descriptorPath, message))
            }
          }
          PluginDescriptorResult.NotFound -> createInvalidPlugin(
            jarPath,
            descriptorPath,
            PluginDescriptorIsNotFound(descriptorPath)
          ).also {
            LOG.debug("Descriptor [{}] not found in [{}]", descriptorPath, jarPath)
          }

          is PluginDescriptorResult.Failed -> createInvalidPlugin(
            jarPath,
            descriptorPath,
            PluginDescriptorIsNotFound(descriptorPath)
          ).also {
            LOG.debug("Unable to resolve descriptor [{}] from [{}] ({})", descriptorPath, jarPath, descriptor)
          }
        }
      }
    } catch (e: JarArchiveCannotBeOpenException) {
      LOG.warn("Unable to extract {} (searching for {}): {}", jarPath, descriptorPath, e.getShortExceptionMessage())
      createInvalidPlugin(jarPath, descriptorPath, UnableToExtractZip())
    }
  }

  fun isLoadable(pluginLoadingContext: Context): Boolean {
    val descriptorPath = FilenameUtils.normalize("$META_INF/${pluginLoadingContext.descriptorPath}")
    return pluginLoadingContext.jarPath.newZipHandler()
      .runCatching {
        handleEntry(descriptorPath) { _, _ -> true } ?: false
      }.getOrElse {
        LOG.debug(it.message)
        false
      }
  }

  internal data class Context(
    val jarPath: Path,
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