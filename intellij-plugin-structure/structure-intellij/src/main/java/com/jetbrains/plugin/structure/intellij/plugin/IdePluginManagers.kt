/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import java.nio.file.Path

fun createIdePluginManager(archiveManager: PluginArchiveManager): IdePluginManager = createIdePluginManager {
  this.pluginArchiveManager = archiveManager
}

fun createIdePluginManager(builder: IdePluginManagerConfiguration.Builder.() -> Unit): IdePluginManager {
  return IdePluginManagerConfiguration.Builder()
    .apply(builder)
    .build()
    .run {
      IdePluginManager.createManager(resourceResolver, pluginArchiveManager, fileSystemProvider)
    }
}

class IdePluginManagerConfiguration(
  val pluginArchiveManager: PluginArchiveManager,
  val resourceResolver: ResourceResolver,
  val fileSystemProvider: JarFileSystemProvider
) {

  class Builder {
    var pluginArchiveManager: PluginArchiveManager? = null
    var extractDirectory: Path? = null
    var resourceResolver: ResourceResolver? = null
    var fileSystemProvider: JarFileSystemProvider? = null

    fun build(): IdePluginManagerConfiguration {
      if (extractDirectory == null && pluginArchiveManager == null) {
        throw IllegalArgumentException("Either a plugin archive manager or an extract directory must be specified")
      }
      val pluginArchiveManager = pluginArchiveManager ?: PluginArchiveManager(extractDirectory!!)
      val resourceResolver = resourceResolver ?: DefaultResourceResolver
      val fileSystemProvider = fileSystemProvider ?: SingletonCachingJarFileSystemProvider
      return IdePluginManagerConfiguration(pluginArchiveManager, resourceResolver, fileSystemProvider)
    }
  }
}


