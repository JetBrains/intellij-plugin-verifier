/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.MultiplePluginDescriptors
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.UnreadableZipOrJarFile
import com.jetbrains.plugin.structure.base.problems.isInvalidDescriptorProblem
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.loaders.JarPluginLoader.Loadability.*
import com.jetbrains.plugin.structure.intellij.plugin.loaders.LibDirectoryPluginLoader.LoadResult.NotLoadable.failed
import com.jetbrains.plugin.structure.intellij.plugin.loaders.LibDirectoryPluginLoader.LoadResult.NotLoadable.loaded
import com.jetbrains.plugin.structure.intellij.plugin.module.ContentModuleScanner
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(LibDirectoryPluginLoader::class.java)

internal class LibDirectoryPluginLoader(
  private val pluginLoaderRegistry: PluginLoaderProvider,
  private val fileSystemProvider: JarFileSystemProvider
) : PluginLoader<LibDirectoryPluginLoader.Context> {

  private val jarLoader: JarPluginLoader
    get() = pluginLoaderRegistry.get<JarPluginLoader.Context, JarPluginLoader>()

  private val directoryLoader: PluginDirectoryLoader
    get() = pluginLoaderRegistry.get<PluginDirectoryLoader.Context, PluginDirectoryLoader>()

  private val contentModuleScanner = ContentModuleScanner(fileSystemProvider)

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

    val archivePaths = jarFiles + files.filter { it.isZip() }
    val archiveResults = archivePaths.mapNotNull { archivePath ->
      //Use the composite resource resolver, which can resolve resources in lib's jar files.
      val loadResult = getArchiveContext(archivePath, compositeResolver, hasDotNetDirectory)
        .loadArchive(pluginLoadingContext)
      when (loadResult) {
        is LoadResult.Loaded -> loadResult.creator
        is LoadResult.Failed -> return loadResult.creator
        LoadResult.NotLoadable -> null
      }
    }
    val dirResults = files.filter { it.isDirectory }.map { dirPath ->
      //Use the common resource resolver, which is unaware of lib's jar files.
      directoryLoader.loadPlugin(PluginDirectoryLoader.Context(
        pluginDirectory = dirPath,
        descriptorPath = descriptorPath,
        validateDescriptor = validateDescriptor,
        resourceResolver = resourceResolver,
        parentPlugin = parentPlugin,
        problemResolver = problemResolver,
        hasDotNetDirectory = hasDotNetDirectory
      ))
    }

    val results = archiveResults + dirResults

    val possibleResults = results
      .filter { it.isSuccess || hasOnlyInvalidDescriptorErrors(it) }
    return when(possibleResults.size) {
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

  private fun JarPluginLoader.Context.loadArchive(parentContext: Context): LoadResult {
    val parentArtifactPath = parentContext.libDirectoryParent
    val loadability = jarLoader.getLoadability(pluginLoadingContext = this)
    return when (loadability) {
      Loadable -> {
        jarLoader.loadPlugin(pluginLoadingContext = this).also {
          logFoundDescriptor(parentArtifactPath, jarPath, descriptorPath)
        }.loaded()
      }
      NotLoadable -> LoadResult.NotLoadable
      is Failed -> {
        createInvalidPlugin(
          parentArtifactPath,
          descriptorPath,
          UnreadableZipOrJarFile.of(jarPath, loadability.exception)
        ).failed()
      }
    }
  }

  private fun PluginCreator.withResolvedClasspath(path: Path): PluginCreator = apply {
    val contentModules = contentModuleScanner.getContentModules(path)
    val classpath = contentModules.asClasspath()
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

  private fun Context.getArchiveContext(jarPath: Path, resolver: ResourceResolver, hasDotNetDirectory: Boolean): JarPluginLoader.Context {
    return JarPluginLoader.Context(
      jarPath,
      descriptorPath,
      validateDescriptor,
      resolver,
      parentPlugin,
      problemResolver,
      hasDotNetDirectory
    )
  }

  private fun logFoundDescriptor(
    libDir: Path,
    jar: Path,
    descriptorPath: String
  ) {
    LOG.debug("Found plugin descriptor [{}] in [{}/{}]", descriptorPath, libDir, jar)
  }

  private sealed class LoadResult {
    data class Loaded(val creator: PluginCreator) : LoadResult()
    object NotLoadable : LoadResult()
    data class Failed(val creator: PluginCreator): LoadResult()

    fun PluginCreator.loaded(): Loaded = Loaded(this)
    fun PluginCreator.failed(): Failed = Failed(this)
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

