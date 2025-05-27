/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.getShortExceptionMessage
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.META_INF
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.jar.JarArchiveCannotBeOpenException
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import com.jetbrains.plugin.structure.jar.PluginJar
import org.jdom2.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(JarPluginLoader::class.java)

internal class JarPluginLoader(private val fileSystemProvider: JarFileSystemProvider) {
  fun loadPlugin(pluginLoadingContext: PluginLoadingContext): PluginCreator = with(pluginLoadingContext) {
    val jarFile = artifactPath
    return try {
      PluginJar(jarFile, fileSystemProvider).use { jar ->
        when (val descriptor = jar.getPluginDescriptor("$META_INF/$descriptorPath")) {
          is Found -> {
            try {
              val descriptorXml = descriptor.loadXml()
              createPlugin(jarFile.simpleName, descriptorPath, parentPlugin, validateDescriptor, descriptorXml, descriptor.path, resourceResolver, problemResolver).apply {
                setIcons(jar.getIcons())
                setThirdPartyDependencies(jar.getThirdPartyDependencies())
                setHasDotNetPart(hasDotNetDirectory)
              }
            } catch (e: Exception) {
              LOG.warn("Unable to read descriptor [$descriptorPath] from [$jarFile]", e)
              val message = e.localizedMessage
              createInvalidPlugin(jarFile, descriptorPath, UnableToReadDescriptor(descriptorPath, message))
            }
          }
          else -> createInvalidPlugin(jarFile, descriptorPath, PluginDescriptorIsNotFound(descriptorPath)).also {
            LOG.debug("Unable to resolve descriptor [{}] from [{}] ({})", descriptorPath, jarFile, descriptor)
          }
        }
      }
    } catch (e: JarArchiveCannotBeOpenException) {
      LOG.warn("Unable to extract {} (searching for {}): {}", jarFile, descriptorPath, e.getShortExceptionMessage())
      createInvalidPlugin(jarFile, descriptorPath, UnableToExtractZip())
    }
  }

  private fun Found.loadXml(): Document {
    return inputStream.use {
      JDOMUtil.loadDocument(it)
    }
  }
}