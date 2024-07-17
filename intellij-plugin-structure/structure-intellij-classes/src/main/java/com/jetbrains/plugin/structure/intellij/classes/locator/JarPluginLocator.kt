/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

/**
 * Locates plugin classes located in a single JAR file.
 */
class JarPluginLocator(
  private val readMode: Resolver.ReadMode,
  private val fileOriginProvider: FileOriginProvider = SingleJarFileOriginProvider
) : ClassesLocator {
  override val locationKey: LocationKey = JarPluginKey

  /**
   * Locates classes in a single JAR via a single [Resolver].
   * @return a single-element list with [Resolver] of this JAR
   */
  override fun findClasses(idePlugin: IdePlugin, pluginFile: Path): List<Resolver> {
    if (pluginFile.isJar()) {
      return listOf(JarFileResolver(pluginFile, readMode, fileOriginProvider.getFileOrigin(idePlugin, pluginFile)))
    }
    return emptyList()
  }
}

object JarPluginKey : LocationKey {
  override val name: String = "jar"

  override fun getLocator(readMode: Resolver.ReadMode) = JarPluginLocator(readMode)
}

object SingleJarFileOriginProvider: FileOriginProvider {
  override fun getFileOrigin(idePlugin: IdePlugin, pluginFile: Path) = PluginFileOrigin.SingleJar(idePlugin)
}