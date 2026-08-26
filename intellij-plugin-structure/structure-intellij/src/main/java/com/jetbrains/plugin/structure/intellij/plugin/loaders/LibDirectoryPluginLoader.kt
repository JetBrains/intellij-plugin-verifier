/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.LibDirJarsClasspathProvider
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.module.ContentModuleScanner
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private const val JAR_ROOT_DESCRIPTOR_PREFIX = "../"

internal class LibDirectoryPluginLoader(
  private val pluginLoaderRegistry: PluginLoaderProvider,
  private val fileSystemProvider: JarFileSystemProvider
) : PluginLoader<LibDirectoryPluginLoader.Context> {

  private val jarLoader: JarPluginLoader
    get() = pluginLoaderRegistry.get<JarPluginLoader.Context, JarPluginLoader>()

  private val directoryLoader: PluginDirectoryLoader
    get() = pluginLoaderRegistry.get<PluginDirectoryLoader.Context, PluginDirectoryLoader>()

  private val contentModuleScanner = ContentModuleScanner(fileSystemProvider)

  private val libDirClasspathProvider = LibDirJarsClasspathProvider()

  /**
   * Descriptor indexes of plugin artifacts that are currently being parsed, see [ContentModuleScanner.getDescriptorIndex].
   *
   * A single plugin artifact parse resolves the plugin descriptor, every content module descriptor and every
   * optional dependency descriptor from the same `lib` directory, so the directory is indexed once per parse.
   * Entries are discarded by [invalidateDescriptorIndex] as soon as the parse completes: an index is of no use
   * for other plugin artifacts, as each of them has its own `lib` directory.
   */
  private val descriptorIndexes = ConcurrentHashMap<Path, Map<String, List<Path>>>()

  override fun loadPlugin(pluginLoadingContext: Context): PluginCreator = with(pluginLoadingContext) {
    val libDir = libDirectoryParent.resolve("lib")
    val hasDotNetDirectory = libDirectoryParent.resolve("dotnet").exists()
    if (!libDir.isDirectory) {
      return createInvalidPlugin(libDirectoryParent, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
    }
    val files = libDir.listFiles()
    if (files.isEmpty()) {
      return createInvalidPlugin(libDirectoryParent, descriptorPath, PluginLibDirectoryIsEmpty())
    }
    val jarFiles = files.filter { it.isJar() }
    val libResourceResolver: ResourceResolver = JarsResourceResolver(jarFiles, fileSystemProvider)
    val compositeResolver: ResourceResolver = CompositeResourceResolver(listOf(libResourceResolver, resourceResolver))

    val results: MutableList<PluginCreator> = ArrayList()
    for (file in getDescriptorProviders(libDirectoryParent, descriptorPath, files)) {
      val innerCreator: PluginCreator = if (file.isJar() || file.isZip()) {
        //Use the composite resource resolver, which can resolve resources in lib's jar files.
        jarLoader.loadPlugin(
          JarPluginLoader.Context(
            file,
            descriptorPath,
            validateDescriptor,
            compositeResolver,
            parentPlugin,
            problemResolver,
            hasDotNetDirectory
          )
        )
      } else if (file.isDirectory) {
        //Use the common resource resolver, which is unaware of lib's jar files.
        directoryLoader.loadPlugin(
          PluginDirectoryLoader.Context(
            pluginDirectory = file,
            descriptorPath = descriptorPath,
            validateDescriptor = validateDescriptor,
            resourceResolver = resourceResolver,
            parentPlugin = parentPlugin,
            problemResolver = problemResolver,
            hasDotNetDirectory = hasDotNetDirectory
          )
        )
      } else {
        continue
      }
      results.add(innerCreator)
    }

    val hardErrorResult = results.firstOrNull { creator ->
      when (val result = creator.pluginCreationResult) {
        is PluginCreationSuccess<*> -> false
        is PluginCreationFail<*> -> result.errorsAndWarnings.any { it is PluginFileError }
      }
    }
    if (hardErrorResult != null) return hardErrorResult

    val possibleResults = results
      .filter { it.isSuccess || hasOnlyInvalidDescriptorErrors(it) }
    return when (possibleResults.size) {
      0 -> createInvalidPlugin(libDirectoryParent, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
      1 -> possibleResults.single().withResolvedClasspath(libDirectoryParent)
      else -> {
        val first = possibleResults[0]
        val second = possibleResults[1]
        val multipleDescriptorsProblem: PluginProblem = MultiplePluginDescriptors(
          first.descriptorPath,
          first.pluginFileName,
          second.descriptorPath,
          second.pluginFileName
        )
        createInvalidPlugin(libDirectoryParent, descriptorPath, multipleDescriptorsProblem)
      }
    }
  }

  /**
   * Discards the descriptor index of [pluginArtifactPath] and of any plugin artifact nested in it.
   * Invoked once a plugin artifact parse completes, see [descriptorIndexes].
   */
  internal fun invalidateDescriptorIndex(pluginArtifactPath: Path) {
    descriptorIndexes.keys.removeIf { it.startsWith(pluginArtifactPath) }
  }

  /**
   * Returns those [files] of the `lib` directory that can provide [descriptorPath]: archives indexed as
   * containing such a descriptor, along with every directory, as directories are not indexed.
   *
   * All [files] are returned for a [descriptorPath] the index cannot answer for, see [getIndexableDescriptorName].
   * The order of [files] is retained, so that an ambiguous descriptor is reported with the same pair of files
   * as when every file is probed.
   */
  private fun getDescriptorProviders(libDirectoryParent: Path, descriptorPath: String, files: List<Path>): List<Path> {
    val descriptorName = getIndexableDescriptorName(descriptorPath) ?: return files
    val index = descriptorIndexes.computeIfAbsent(libDirectoryParent) { contentModuleScanner.getDescriptorIndex(it) }
    val archives: Set<Path> = index[descriptorName]?.toHashSet() ?: emptySet()
    return files.filter { it in archives || it.isDirectory }
  }

  /**
   * Returns the file name under which [descriptorPath] is indexed, or `null` if it is not indexed at all.
   *
   * A JAR is searched for a descriptor in its `META-INF` directory, hence a `../` prefix escapes `META-INF`
   * and denotes a descriptor in a JAR root. Such content module descriptors are the only indexed ones:
   * a plugin artifact is parsed once, so indexing its `lib` directory to resolve a single `META-INF` descriptor
   * would scan the very JARs the index saves from being scanned. Any other descriptor path may also resolve
   * to an arbitrary JAR entry, which the index does not cover.
   */
  private fun getIndexableDescriptorName(descriptorPath: String): String? =
    if (descriptorPath.startsWith(JAR_ROOT_DESCRIPTOR_PREFIX)) {
      descriptorPath.removePrefix(JAR_ROOT_DESCRIPTOR_PREFIX).takeIf { it.isNotEmpty() && '/' !in it }
    } else {
      null
    }

  private fun PluginCreator.withResolvedClasspath(path: Path): PluginCreator = apply {
    val libDirClasspath = libDirClasspathProvider.getClasspath(path)
    val contentModuleClasspath = if (plugin.contentModules.isEmpty()) {
      Classpath.EMPTY
    } else {
      contentModuleScanner.getContentModules(path).asClasspath()
    }

    val classpath = contentModuleClasspath.mergeWith(libDirClasspath)

    setClasspath(classpath.getUnique())
  }

  private fun hasOnlyInvalidDescriptorErrors(creator: PluginCreator): Boolean {
    return when (val pluginCreationResult = creator.pluginCreationResult) {
      is PluginCreationSuccess<*> -> false
      is PluginCreationFail<*> -> {
        val errorsAndWarnings = pluginCreationResult.errorsAndWarnings
        errorsAndWarnings.all { it.level !== PluginProblem.Level.ERROR || it.isInvalidDescriptorProblem }
      }
    }
  }

  internal data class Context(
    val libDirectoryParent: Path,
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
