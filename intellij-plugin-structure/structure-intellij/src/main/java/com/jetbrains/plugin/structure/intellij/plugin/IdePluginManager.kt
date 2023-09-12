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
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.input.JDOMParseException
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.util.*
import java.util.stream.Collectors

/**
 * Factory for plugin of the IntelliJ Platform.
 *
 * Handles the plugin provided in JAR, ZIP or directory.
 */
class IdePluginManager private constructor(
  private val myResourceResolver: ResourceResolver,
  private val extractDirectory: Path
) : PluginManager<IdePlugin> {

  private val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

  private fun loadPluginInfoFromJarFile(
    jarFile: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?
  ): PluginCreator {
    val zipFs = try {
      FileSystems.newFileSystem(jarFile, IdePluginManager::class.java.classLoader)
    } catch (e: Throwable) {
      LOG.warn("Unable to extract $jarFile (searching for $descriptorPath): ${e.getShortExceptionMessage()}")
      return createInvalidPlugin(jarFile, descriptorPath, UnableToExtractZip())
    }
    try {
      zipFs.use { jarFileSystem ->
        val entryName = "$META_INF/$descriptorPath"
        val entry = jarFileSystem.getPath(toCanonicalPath(entryName))
        return if (Files.exists(entry)) {
          try {
            val document = Files.newInputStream(entry).use { JDOMUtil.loadDocument(it) }
            val icons = getIconsFromJarFile(jarFileSystem)
            val dependencies = getThirdPartyDependenciesFromJarFile(jarFileSystem)
            val plugin = createPlugin(jarFile, descriptorPath, parentPlugin, validateDescriptor, document, entry, resourceResolver)
            plugin.setIcons(icons)
            plugin.setThirdPartyDependencies(dependencies)
            plugin
          } catch (e: Exception) {
            LOG.info("Unable to read file $descriptorPath", e)
            val message = e.localizedMessage
            createInvalidPlugin(jarFile, descriptorPath, UnableToReadDescriptor(descriptorPath, message))
          }
        } else {
          createInvalidPlugin(jarFile, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
        }
      }
    } catch (e: Exception) {
      LOG.warn("Unable to read $jarFile in search of $descriptorPath: ${e.message}")
      return createInvalidPlugin(jarFile, descriptorPath, UnableToExtractZip())
    }
  }

  private fun getThirdPartyDependenciesFromJarFile(jarFileSystem: FileSystem): List<ThirdPartyDependency> {
    val path = jarFileSystem.getPath(META_INF, THIRD_PARTY_LIBRARIES_FILE_NAME)
    return parseThirdPartyDependenciesByPath(path)
  }

  private fun getIconsFromJarFile(jarFileSystem: FileSystem): List<PluginIcon> =
    IconTheme.values().mapNotNull { theme ->
      val iconEntryName = "$META_INF/${getIconFileName(theme)}"
      val iconPath = jarFileSystem.getPath(META_INF, getIconFileName(theme))
      if (iconPath.exists()) {
        PluginIcon(theme, iconPath.readBytes(), iconEntryName)
      } else {
        null
      }
    }

  private fun loadPluginInfoFromDirectory(
    pluginDirectory: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?
  ): PluginCreator {
    val descriptorFile = pluginDirectory.resolve(META_INF).resolve(descriptorPath.withPathSeparatorOf(pluginDirectory))
    return if (!descriptorFile.exists()) {
      loadPluginInfoFromLibDirectory(pluginDirectory, descriptorPath, validateDescriptor, resourceResolver, parentPlugin)
    } else try {
      val document = JDOMUtil.loadDocument(Files.newInputStream(descriptorFile))
      val icons = loadIconsFromDir(pluginDirectory)
      val dependencies = getThirdPartyDependenciesFromDir(pluginDirectory)
      val plugin = createPlugin(
        pluginDirectory, descriptorPath, parentPlugin, validateDescriptor, document, descriptorFile, resourceResolver
      )
      plugin.setIcons(icons)
      plugin.setThirdPartyDependencies(dependencies)
      plugin
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
    parentPlugin: PluginCreator?
  ): PluginCreator {
    val libDir = root.resolve("lib")
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
        loadPluginInfoFromJarFile(file, descriptorPath, validateDescriptor, compositeResolver, parentPlugin)
      } else if (file.isDirectory) {
        //Use the common resource resolver, which is unaware of lib's jar files.
        loadPluginInfoFromDirectory(file, descriptorPath, validateDescriptor, resourceResolver, parentPlugin)
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
    parentPlugin: PluginCreator?
  ): PluginCreator {
    val systemIndependentDescriptorPath = descriptorPath.toSystemIndependentName()
    return when {
      pluginFile.isDirectory -> {
        loadPluginInfoFromDirectory(pluginFile, systemIndependentDescriptorPath, validateDescriptor, resourceResolver, parentPlugin)
      }

      pluginFile.isJar() -> {
        loadPluginInfoFromJarFile(pluginFile, systemIndependentDescriptorPath, validateDescriptor, resourceResolver, parentPlugin)
      }

      else -> throw IllegalArgumentException()
    }
  }

  private fun resolveOptionalDependencies(pluginFile: Path, pluginCreator: PluginCreator, resourceResolver: ResourceResolver) {
    if (pluginCreator.isSuccess) {
      resolveOptionalDependencies(pluginCreator, HashSet(), LinkedList(), pluginFile, resourceResolver, pluginCreator)
    }
  }

  private fun resolveContentModules(pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver) {
    if (currentPlugin.isSuccess) {
      val contentModules = currentPlugin.contentModules
      for (module in contentModules) {
        val configFile = module.configFile
        val moduleCreator = loadPluginInfoFromJarOrDirectory(pluginFile, configFile, false, resourceResolver, currentPlugin)
        currentPlugin.addModuleDescriptor(module.name, configFile, moduleCreator)
      }
    }

  }

  /**
   * [mainPlugin] - the root plugin (plugin.xml)
   * [currentPlugin] - plugin whose optional dependencies are resolved (plugin.xml, then someOptional.xml, ...)
   */
  private fun resolveOptionalDependencies(
    currentPlugin: PluginCreator,
    visitedConfigurationFiles: MutableSet<String>,
    path: LinkedList<String>,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    mainPlugin: PluginCreator
  ) {
    if (!visitedConfigurationFiles.add(currentPlugin.descriptorPath)) {
      return
    }
    path.addLast(currentPlugin.descriptorPath)
    val optionalDependenciesConfigFiles: Map<PluginDependency, String> = currentPlugin.optionalDependenciesConfigFiles
    for ((pluginDependency, configurationFile) in optionalDependenciesConfigFiles) {
      if (path.contains(configurationFile)) {
        val configurationFilesCycle: MutableList<String> = ArrayList(path)
        configurationFilesCycle.add(configurationFile)
        mainPlugin.registerOptionalDependenciesConfigurationFilesCycleProblem(configurationFilesCycle)
        return
      }
      val optionalDependencyCreator = loadPluginInfoFromJarOrDirectory(pluginFile, configurationFile, false, resourceResolver, currentPlugin)
      currentPlugin.addOptionalDescriptor(pluginDependency, configurationFile, optionalDependencyCreator)
      resolveOptionalDependencies(optionalDependencyCreator, visitedConfigurationFiles, path, pluginFile, resourceResolver, mainPlugin)
    }
    path.removeLast()
  }

  private fun extractZipAndCreatePlugin(
    pluginFile: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver
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
          val pluginCreator = loadPluginInfoFromJarOrDirectory(extractedFile, descriptorPath, validateDescriptor, resourceResolver, null)
          resolveOptionalDependencies(extractedFile, pluginCreator, myResourceResolver)
          resolveContentModules(extractedFile, pluginCreator, myResourceResolver)
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
    descriptorPath: String = PLUGIN_XML
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, validateDescriptor, descriptorPath)
    return pluginCreator.pluginCreationResult
  }

  fun createBundledPlugin(
    pluginFile: Path,
    ideVersion: IdeVersion,
    descriptorPath: String
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, false, descriptorPath)
    pluginCreator.setPluginVersion(ideVersion.asStringWithoutProductCode())
    return pluginCreator.pluginCreationResult
  }

  private fun getPluginCreatorWithResult(
    pluginFile: Path,
    validateDescriptor: Boolean,
    descriptorPath: String
  ): PluginCreator {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    val pluginCreator: PluginCreator
    if (pluginFile.isZip()) {
      pluginCreator = extractZipAndCreatePlugin(
        pluginFile,
        descriptorPath,
        validateDescriptor,
        myResourceResolver
      )
    } else if (pluginFile.isJar() || pluginFile.isDirectory) {
      pluginCreator = loadPluginInfoFromJarOrDirectory(pluginFile, descriptorPath, validateDescriptor, myResourceResolver, null)
      resolveOptionalDependencies(pluginFile, pluginCreator, myResourceResolver)
      resolveContentModules(pluginFile, pluginCreator, myResourceResolver)
    } else {
      pluginCreator = getInvalidPluginFileCreator(pluginFile.simpleName, descriptorPath)
    }
    pluginCreator.setOriginalFile(pluginFile)
    return pluginCreator
  }

  private fun getInvalidPluginFileCreator(pluginFileName: String, descriptorPath: String): PluginCreator {
    return createInvalidPlugin(pluginFileName, descriptorPath, IncorrectZipOrJarFile(pluginFileName))
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
          errorsAndWarnings.all { it.level !== PluginProblem.Level.ERROR || it is InvalidDescriptorProblem }
        }
      }
    }

    private fun toCanonicalPath(descriptorPath: String): String {
      return Paths.get(descriptorPath.toSystemIndependentName()).normalize().toString()
    }

    private fun getIconFileName(iconTheme: IconTheme) = "pluginIcon${iconTheme.suffix}.svg"
  }
}