/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor.extractPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.*
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import org.jdom2.Document
import org.jdom2.input.JDOMParseException
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.time.Duration
import java.util.*
import java.util.stream.Collectors
import kotlin.system.measureTimeMillis

/**
 * Factory for plugin of the IntelliJ Platform.
 *
 * Handles the plugin provided in JAR, ZIP or directory.
 */
class IdePluginManager private constructor(
  private val myResourceResolver: ResourceResolver,
  private val extractDirectory: Path,
  private val fileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider
) : PluginManager<IdePlugin> {

  private val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

  private val optionalDependencyResolver = OptionalDependencyResolver(PluginLoader { ctx: PluginMetadataResolutionContext ->
    with(ctx) {
      loadPluginInfoFromJarOrDirectory(pluginFile, descriptorPath, false, resourceResolver, parentPlugin, problemResolver)
    }
  })

  private fun loadPluginInfoFromJarFile(
    jarFile: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?,
    problemResolver: PluginCreationResultResolver,
    hasDotNetDirectory: Boolean = false
  ): PluginCreator {

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

   private fun loadPluginInfoFromDirectory(
    pluginDirectory: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?,
    problemResolver: PluginCreationResultResolver,
    hasDotNetDirectory: Boolean = false
  ): PluginCreator {
    val descriptorFile = pluginDirectory.resolve(META_INF).resolve(descriptorPath.withPathSeparatorOf(pluginDirectory))
    return if (!descriptorFile.exists()) {
      loadPluginInfoFromLibDirectory(pluginDirectory,
        descriptorPath,
        validateDescriptor,
        resourceResolver,
        parentPlugin,
        problemResolver)
    } else try {
      val document = JDOMUtil.loadDocument(Files.newInputStream(descriptorFile))
      val icons = loadIconsFromDir(pluginDirectory)
      val dependencies = getThirdPartyDependenciesFromDir(pluginDirectory)
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

  private fun getThirdPartyDependenciesFromDir(pluginDirectory: Path): List<ThirdPartyDependency> {
    val path = pluginDirectory.resolve(META_INF).resolve(THIRD_PARTY_LIBRARIES_FILE_NAME)
    return parseThirdPartyDependenciesByPath(path)
  }


  @Throws(IOException::class)
  private fun loadIconsFromDir(pluginDirectory: Path): List<PluginIcon> {
    return IconTheme.values().mapNotNull { theme ->
      val iconFile = pluginDirectory.resolve(META_INF).resolve(getIconFileName(theme))
      if (iconFile.exists()) {
        PluginIcon(theme, Files.readAllBytes(iconFile), iconFile.simpleName)
      } else {
        null
      }
    }
  }

  private fun loadPluginInfoFromLibDirectory(
    root: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    val libDir = root.resolve("lib")
    val hasDotNetDirectory = root.resolve("dotnet").exists()
    if (!libDir.isDirectory) {
      return createInvalidPlugin(root, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
    }
    val files = libDir.listFiles()
    if (files.isEmpty()) {
      return createInvalidPlugin(root, descriptorPath, PluginLibDirectoryIsEmpty())
    }
    val jarFiles = files.filter { it.isJar() }
    val libResourceResolver: ResourceResolver = JarFilesResourceResolver(jarFiles)
    val compositeResolver: ResourceResolver = CompositeResourceResolver(listOf(libResourceResolver, resourceResolver))
    val results: MutableList<PluginCreator> = ArrayList()
    for (file in files) {
      val innerCreator: PluginCreator = if (file.isJar() || file.isZip()) {
        //Use the composite resource resolver, which can resolve resources in lib's jar files.
        loadPluginInfoFromJarFile(
            jarFile = file,
            descriptorPath = descriptorPath,
            validateDescriptor = validateDescriptor,
            resourceResolver = compositeResolver,
            parentPlugin = parentPlugin,
            problemResolver = problemResolver,
            hasDotNetDirectory = hasDotNetDirectory
        )
      } else if (file.isDirectory) {
        //Use the common resource resolver, which is unaware of lib's jar files.
        loadPluginInfoFromDirectory(
            pluginDirectory = file,
            descriptorPath = descriptorPath,
            validateDescriptor = validateDescriptor,
            resourceResolver = resourceResolver,
            parentPlugin = parentPlugin,
            problemResolver = problemResolver,
            hasDotNetDirectory = hasDotNetDirectory
        )
      } else {
        continue
      }
      results.add(innerCreator)
    }
    val possibleResults = results.stream()
      .filter { r: PluginCreator -> r.isSuccess || hasOnlyInvalidDescriptorErrors(r) }
      .collect(Collectors.toList())
    return when(possibleResults.size) {
      0 -> createInvalidPlugin(root, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
      1 -> possibleResults[0]
      else -> {
        val first = possibleResults[0]
        val second = possibleResults[1]
        val multipleDescriptorsProblem: PluginProblem = MultiplePluginDescriptors(
                first.descriptorPath,
                first.pluginFileName,
                second.descriptorPath,
                second.pluginFileName
        )
        createInvalidPlugin(root, descriptorPath, multipleDescriptorsProblem)
      }
    }
  }

  private fun loadPluginInfoFromJarOrDirectory(
    pluginFile: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    LOG.debug("Loading {} with descriptor [{}]", pluginFile, descriptorPath)
    val systemIndependentDescriptorPath = descriptorPath.toSystemIndependentName()
    return when {
      pluginFile.isDirectory -> {
        loadPluginInfoFromDirectory(pluginFile,
          systemIndependentDescriptorPath,
          validateDescriptor,
          resourceResolver,
          parentPlugin,
          problemResolver)
      }

      pluginFile.isJar() -> {
        loadPluginInfoFromJarFile(pluginFile,
          systemIndependentDescriptorPath,
          validateDescriptor,
          resourceResolver,
          parentPlugin,
          problemResolver)
      }

      else -> throw IllegalArgumentException()
    }
  }

  private fun resolveOptionalDependencies(pluginFile: Path, pluginCreator: PluginCreator, resourceResolver: ResourceResolver, problemResolver: PluginCreationResultResolver) {
    if (pluginCreator.isSuccess) {
      optionalDependencyResolver.resolveOptionalDependencies(pluginCreator, HashSet(), LinkedList(), pluginFile, resourceResolver, problemResolver, pluginCreator)
    }
  }

  private fun resolveContentModules(pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver, problemResolver: PluginCreationResultResolver) {
    if (currentPlugin.isSuccess) {
      val contentModules = currentPlugin.contentModules
      for (module in contentModules) {
        val configFile = module.configFile
        val moduleCreator = loadPluginInfoFromJarOrDirectory(pluginFile, configFile, false, resourceResolver, currentPlugin, problemResolver)
        currentPlugin.addModuleDescriptor(module.name, configFile, moduleCreator)
      }
    }
  }

  private fun extractZipAndCreatePlugin(
    pluginFile: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    val extractorResult = try {
      extractPlugin(pluginFile, extractDirectory)
    } catch (e: Exception) {
      LOG.info("Unable to extract plugin zip ${pluginFile.simpleName}", e)
      return createInvalidPlugin(pluginFile.simpleName, descriptorPath, UnableToExtractZip())
    }
    return when (extractorResult) {
      is ExtractorResult.Success -> extractorResult.extractedPlugin.use { (extractedFile) ->
        if (extractedFile.isJar() || extractedFile.isDirectory) {
          val pluginCreator = loadPluginInfoFromJarOrDirectory(extractedFile, descriptorPath, validateDescriptor, resourceResolver, null, problemResolver)
          resolveOptionalDependencies(extractedFile, pluginCreator, myResourceResolver, problemResolver)
          resolveContentModules(extractedFile, pluginCreator, myResourceResolver, problemResolver)
          pluginCreator
        } else {
          getInvalidPluginFileCreator(pluginFile.simpleName, descriptorPath)
        }
      }

      is ExtractorResult.Fail -> createInvalidPlugin(pluginFile.simpleName, descriptorPath, extractorResult.pluginProblem)
    }
  }

  override fun createPlugin(pluginFile: Path) = createPlugin(pluginFile, true)

  fun createPlugin(
    pluginFile: Path,
    validateDescriptor: Boolean,
    descriptorPath: String = PLUGIN_XML,
    problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, validateDescriptor, descriptorPath, problemResolver)
    return pluginCreator.pluginCreationResult
  }

  fun createBundledPlugin(
    pluginFile: Path,
    ideVersion: IdeVersion,
    descriptorPath: String,
    problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, false, descriptorPath, problemResolver)
    pluginCreator.setPluginVersion(ideVersion.asStringWithoutProductCode())
    return pluginCreator.pluginCreationResult
  }

  private fun getPluginCreatorWithResult(
    pluginFile: Path,
    validateDescriptor: Boolean,
    descriptorPath: String,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    val pluginCreator: PluginCreator
    measureTimeMillis {
      if (pluginFile.isZip()) {
        pluginCreator = extractZipAndCreatePlugin(
          pluginFile,
          descriptorPath,
          validateDescriptor,
          myResourceResolver,
          problemResolver
        )
      } else if (pluginFile.isJar() || pluginFile.isDirectory) {
        pluginCreator = loadPluginInfoFromJarOrDirectory(pluginFile, descriptorPath, validateDescriptor, myResourceResolver, null, problemResolver)
        resolveOptionalDependencies(pluginFile, pluginCreator, myResourceResolver, problemResolver)
        resolveContentModules(pluginFile, pluginCreator, myResourceResolver, problemResolver)
      } else {
        pluginCreator = getInvalidPluginFileCreator(pluginFile.simpleName, descriptorPath)
      }
      pluginCreator.setOriginalFile(pluginFile)
    }.let { pluginCreationDuration -> pluginCreator.setTelemetry(pluginFile, pluginCreationDuration)}
    return pluginCreator
  }

  private fun getInvalidPluginFileCreator(pluginFileName: String, descriptorPath: String): PluginCreator {
    return createInvalidPlugin(pluginFileName, descriptorPath, IncorrectZipOrJarFile(pluginFileName))
  }

  private fun PluginCreator.setTelemetry(pluginFile: Path, pluginCreationDurationInMillis: Long) {
    with(telemetry) {
      parsingDuration = Duration.ofMillis(pluginCreationDurationInMillis)
      archiveFileSize = pluginFile.pluginSize
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(IdePluginManager::class.java)
    const val PLUGIN_XML = "plugin.xml"
    const val META_INF = "META-INF"

    @JvmStatic
    fun createManager(): IdePluginManager =
      createManager(DefaultResourceResolver, Settings.EXTRACT_DIRECTORY.getAsPath())

    @JvmStatic
    fun createManager(resourceResolver: ResourceResolver): IdePluginManager =
      createManager(resourceResolver, Settings.EXTRACT_DIRECTORY.getAsPath())

    @JvmStatic
    fun createManager(extractDirectory: Path): IdePluginManager =
      createManager(DefaultResourceResolver, extractDirectory)

    @JvmStatic
    fun createManager(resourceResolver: ResourceResolver, extractDirectory: Path): IdePluginManager =
      IdePluginManager(resourceResolver, extractDirectory)

    @JvmStatic
    fun createManager(resourceResolver: ResourceResolver, extractDirectory: Path, fileSystemProvider: JarFileSystemProvider): IdePluginManager =
      IdePluginManager(resourceResolver, extractDirectory, fileSystemProvider)

    @Deprecated(
      message = "Use factory method with java.nio.Path",
      replaceWith = ReplaceWith("createManager(extractDirectory.toPath())")
    )
    @JvmStatic
    fun createManager(extractDirectory: File): IdePluginManager =
      createManager(DefaultResourceResolver, extractDirectory.toPath())

    @Deprecated(
      message = "Use factory method with java.nio.Path",
      replaceWith = ReplaceWith("createManager(resourceResolver, extractDirectory.toPath())")
    )
    @JvmStatic
    fun createManager(resourceResolver: ResourceResolver, extractDirectory: File): IdePluginManager =
      createManager(resourceResolver, extractDirectory.toPath())

    private fun hasOnlyInvalidDescriptorErrors(creator: PluginCreator): Boolean {
      return when (val pluginCreationResult = creator.pluginCreationResult) {
        is PluginCreationSuccess<*> -> false
        is PluginCreationFail<*> -> {
          val errorsAndWarnings = pluginCreationResult.errorsAndWarnings
          errorsAndWarnings.all { it.level !== PluginProblem.Level.ERROR || it.isInvalidDescriptorProblem }
        }
      }
    }

    private fun getIconFileName(iconTheme: IconTheme) = "pluginIcon${iconTheme.suffix}.svg"
  }
}