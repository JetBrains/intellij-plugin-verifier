/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.jps

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.ide.AbstractIdeManager
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.resources.CompiledModulesResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import java.io.IOException
import java.nio.file.Path

class CompiledIdeManager(private val jarFileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider) : AbstractIdeManager() {
  override fun createIde(idePath: Path): Ide = createIde(idePath, null)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    if (!idePath.isDirectory) {
      throw IOException("Specified path does not exist or is not a directory: $idePath")
    }
    if (!supports(idePath, version)) {
      throw InvalidIdeException(idePath, "IDE directory content is invalid")
    }

    val readIdeVersion = version ?: readIdeVersion(idePath)
    val ideVersion = resolveProductSpecificVersion(idePath, readIdeVersion)

    val bundledPlugins = readCompiledBundledPlugins(idePath, ideVersion)

    return CompiledIde(idePath, ideVersion, bundledPlugins)
  }

  private fun readCompiledBundledPlugins(idePath: Path, ideVersion: IdeVersion): List<IdePlugin> {
    val compilationRoot = getCompiledClassesRoot(idePath)!!
    val moduleRoots = compilationRoot.listFiles().toList()
    val librariesJars = getRepositoryLibrariesJars(idePath)
    val pathResolver = CompositeResourceResolver(
      listOf(
        CompiledModulesResourceResolver(moduleRoots),
        JarsResourceResolver(librariesJars, jarFileSystemProvider)
      )
    )
    return readCompiledBundledPlugins(idePath, moduleRoots, pathResolver, ideVersion)
  }

  private fun readCompiledBundledPlugins(
    idePath: Path,
    moduleRoots: List<Path>,
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): List<IdePlugin> {
    val plugins = arrayListOf<IdePlugin>()
    for (moduleRoot in moduleRoots) {
      val pluginXmlFile = moduleRoot.resolve(IdePluginManager.META_INF).resolve(IdePluginManager.PLUGIN_XML)
      if (pluginXmlFile.isFile) {
        plugins += createBundledPluginExceptionally(idePath, moduleRoot, pathResolver, IdePluginManager.PLUGIN_XML, ideVersion)
      }
    }
    return plugins
  }

  private fun readIdeVersion(idePath: Path): IdeVersion {
    val locations = listOf(
      idePath.resolve("build.txt"),
      idePath.resolve("community").resolve("build.txt")
    )
    val buildTxtFile = locations.find { it.exists() }
      ?: throw InvalidIdeException(idePath, "Unable to find IDE version file 'build.txt' or 'community/build.txt'")
    return readBuildNumber(buildTxtFile)
  }

  override fun supports(idePath: Path, version: IdeVersion?): Boolean {
    return isCompiledCommunity(idePath)
      || isCompiledUltimate(idePath)
  }
}