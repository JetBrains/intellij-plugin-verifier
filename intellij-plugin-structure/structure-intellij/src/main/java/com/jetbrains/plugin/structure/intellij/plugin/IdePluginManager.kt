/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor.extractPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.intellij.problems.UnableToReadJarFile
import com.jetbrains.plugin.structure.intellij.problems.createIncorrectIntellijFileProblem
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.apache.commons.io.IOUtils
import org.jdom2.input.JDOMParseException
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class IdePluginManager private constructor(
  private val myResourceResolver: ResourceResolver,
  private val myExtractDirectory: File
) : PluginManager<IdePlugin> {
  private fun loadPluginInfoFromJarFile(
    jarFile: File,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?
  ): PluginCreator {
    val zipFile: ZipFile
    zipFile = try {
      ZipFile(jarFile)
    } catch (e: Exception) {
      LOG.info("Unable to read jar file $jarFile", e)
      return createInvalidPlugin(jarFile, descriptorPath, UnableToReadJarFile())
    }
    try {
      val entryName = "$META_INF/$descriptorPath"
      val entry = getZipEntry(zipFile, toCanonicalPath(entryName))
      if (entry != null) {
        try {
          zipFile.getInputStream(entry).use { documentStream ->
            if (documentStream == null) {
              return createInvalidPlugin(jarFile, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
            }
            val document = JDOMUtil.loadDocument(documentStream)
            val icons = getIconsFromJarFile(zipFile)
            val documentUrl = URLUtil.getJarEntryURL(jarFile, entry.name)
            val plugin = createPlugin(jarFile, descriptorPath, parentPlugin, validateDescriptor, document, documentUrl, resourceResolver)
            plugin.setIcons(icons)
            return plugin
          }
        } catch (e: Exception) {
          LOG.info("Unable to read file $descriptorPath", e)
          val message = e.localizedMessage
          return createInvalidPlugin(jarFile, descriptorPath, UnableToReadDescriptor(descriptorPath, message))
        }
      } else {
        return createInvalidPlugin(jarFile, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
      }
    } finally {
      try {
        zipFile.close()
      } catch (e: IOException) {
        LOG.error("Unable to close jar file $jarFile", e)
      }
    }
  }

  @Throws(IOException::class)
  private fun getIconsFromJarFile(jarFile: ZipFile): List<PluginIcon> {
    val icons: MutableList<PluginIcon> = ArrayList()
    for (theme in IconTheme.values()) {
      val iconEntryName = META_INF + "/" + getIconFileName(theme)
      val entry = getZipEntry(jarFile, toCanonicalPath(iconEntryName)) ?: continue
      val iconStream = jarFile.getInputStream(entry) ?: continue
      iconStream.use {
        val iconContent = ByteArray(entry.size.toInt())
        IOUtils.readFully(it, iconContent)
        icons.add(PluginIcon(theme, iconContent, iconEntryName))
      }
    }
    return icons
  }

  private fun loadPluginInfoFromDirectory(
    pluginDirectory: File,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?
  ): PluginCreator {
    val descriptorFile = File(File(pluginDirectory, META_INF), descriptorPath.toSystemIndependentName())
    return if (!descriptorFile.exists()) {
      loadPluginInfoFromLibDirectory(pluginDirectory, descriptorPath, validateDescriptor, resourceResolver, parentPlugin)
    } else try {
      val documentUrl = URLUtil.fileToUrl(descriptorFile)
      val document = JDOMUtil.loadDocument(documentUrl)
      val icons = loadIconsFromDir(pluginDirectory)
      val plugin = createPlugin(pluginDirectory, descriptorPath, parentPlugin, validateDescriptor, document, documentUrl, resourceResolver)
      plugin.setIcons(icons)
      plugin
    } catch (e: JDOMParseException) {
      val lineNumber = e.lineNumber
      val message = if (lineNumber != -1) "unexpected element on line $lineNumber" else "unexpected elements"
      LOG.info("Unable to parse plugin descriptor $descriptorPath of plugin $descriptorFile", e)
      createInvalidPlugin(pluginDirectory, descriptorPath, UnexpectedDescriptorElements(message, descriptorPath))
    } catch (e: Exception) {
      LOG.info("Unable to read plugin descriptor $descriptorPath of plugin $descriptorFile", e)
      createInvalidPlugin(pluginDirectory, descriptorPath, UnableToReadDescriptor(descriptorPath, descriptorPath))
    }
  }

  @Throws(IOException::class)
  private fun loadIconsFromDir(pluginDirectory: File): List<PluginIcon> {
    val icons: MutableList<PluginIcon> = ArrayList()
    for (theme in IconTheme.values()) {
      val iconFile = File(File(pluginDirectory, META_INF), getIconFileName(theme).toSystemIndependentName())
      if (!iconFile.exists()) {
        continue
      }
      val iconContent = ByteArray(iconFile.length().toInt())
      IOUtils.readFully(FileInputStream(iconFile), iconContent)
      icons.add(PluginIcon(theme, iconContent, iconFile.name))
    }
    return icons
  }

  private fun loadPluginInfoFromLibDirectory(
    root: File,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?
  ): PluginCreator {
    val libDir = File(root, "lib")
    if (!libDir.isDirectory) {
      return createInvalidPlugin(root, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
    }
    val files = libDir.listFiles()
    if (files == null || files.isEmpty()) {
      return createInvalidPlugin(root, descriptorPath, PluginLibDirectoryIsEmpty())
    }
    putMoreLikelyPluginJarsFirst(root, files)
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
    if (possibleResults.size > 1) {
      val first = possibleResults[0]
      val second = possibleResults[1]
      val multipleDescriptorsProblem: PluginProblem = MultiplePluginDescriptors(
        first.descriptorPath,
        first.pluginFile.name,
        second.descriptorPath,
        second.pluginFile.name
      )
      return createInvalidPlugin(root, descriptorPath, multipleDescriptorsProblem)
    }
    return if (possibleResults.size == 1) {
      possibleResults[0]
    } else createInvalidPlugin(root, descriptorPath, PluginDescriptorIsNotFound(descriptorPath))
  }

  private fun loadPluginInfoFromJarOrDirectory(
    pluginFile: File,
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
      else -> {
        throw IllegalArgumentException()
      }
    }
  }

  private fun resolveOptionalDependencies(pluginFile: File, pluginCreator: PluginCreator, resourceResolver: ResourceResolver) {
    if (pluginCreator.isSuccess) {
      resolveOptionalDependencies(pluginCreator, HashSet(), LinkedList(), pluginFile, resourceResolver, pluginCreator)
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
    pluginFile: File,
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
    zipPlugin: File,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver
  ): PluginCreator {
    val extractorResult = try {
      extractPlugin(zipPlugin.inputStream(), myExtractDirectory)
    } catch (e: Exception) {
      LOG.info("Unable to extract plugin zip $zipPlugin", e)
      return createInvalidPlugin(zipPlugin, descriptorPath, UnableToExtractZip())
    }
    return when (extractorResult) {
      is ExtractorResult.Success -> extractorResult.extractedPlugin.use { (extractedFile) ->
        if (extractedFile.isJar() || extractedFile.isDirectory) {
          val pluginCreator = loadPluginInfoFromJarOrDirectory(extractedFile, descriptorPath, validateDescriptor, resourceResolver, null)
          resolveOptionalDependencies(extractedFile, pluginCreator, myResourceResolver)
          pluginCreator
        } else {
          getInvalidPluginFileCreator(zipPlugin, descriptorPath)
        }
      }
      is ExtractorResult.Fail -> createInvalidPlugin(zipPlugin, descriptorPath, extractorResult.pluginProblem)
    }
  }

  override fun createPlugin(pluginFile: File): PluginCreationResult<IdePlugin> {
    return createPlugin(pluginFile, true)
  }

  @JvmOverloads
  fun createPlugin(
    pluginFile: File,
    validateDescriptor: Boolean,
    descriptorPath: String = PLUGIN_XML
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, validateDescriptor, descriptorPath)
    return pluginCreator.pluginCreationResult
  }

  fun createBundledPlugin(
    pluginFile: File,
    ideVersion: IdeVersion,
    descriptorPath: String
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, false, descriptorPath)
    pluginCreator.setPluginVersion(ideVersion.asStringWithoutProductCode())
    return pluginCreator.pluginCreationResult
  }

  private fun getPluginCreatorWithResult(
    pluginFile: File,
    validateDescriptor: Boolean,
    descriptorPath: String
  ): PluginCreator {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    val pluginCreator: PluginCreator
    if (pluginFile.isZip()) {
      pluginCreator = extractZipAndCreatePlugin(pluginFile, descriptorPath, validateDescriptor, myResourceResolver)
    } else if (pluginFile.isJar() || pluginFile.isDirectory) {
      pluginCreator = loadPluginInfoFromJarOrDirectory(pluginFile, descriptorPath, validateDescriptor, myResourceResolver, null)
      resolveOptionalDependencies(pluginFile, pluginCreator, myResourceResolver)
    } else {
      pluginCreator = getInvalidPluginFileCreator(pluginFile, descriptorPath)
    }
    pluginCreator.setOriginalFile(pluginFile)
    return pluginCreator
  }

  private fun getInvalidPluginFileCreator(pluginFile: File, descriptorPath: String): PluginCreator {
    return createInvalidPlugin(pluginFile, descriptorPath, createIncorrectIntellijFileProblem(pluginFile.name))
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(IdePluginManager::class.java)
    const val PLUGIN_XML = "plugin.xml"
    const val META_INF = "META-INF"
    fun createManager(extractDirectory: File): IdePluginManager {
      return createManager(DefaultResourceResolver, extractDirectory)
    }

    @JvmOverloads
    fun createManager(resourceResolver: ResourceResolver = DefaultResourceResolver, extractDirectory: File = Settings.EXTRACT_DIRECTORY.getAsFile()): IdePluginManager {
      return IdePluginManager(resourceResolver, extractDirectory)
    }

    private fun hasOnlyInvalidDescriptorErrors(creator: PluginCreator): Boolean {
      return when (val pluginCreationResult = creator.pluginCreationResult) {
        is PluginCreationSuccess<*> -> false
        is PluginCreationFail<*> -> {
          val errorsAndWarnings = pluginCreationResult.errorsAndWarnings
          errorsAndWarnings.all { it.level !== PluginProblem.Level.ERROR || it is InvalidDescriptorProblem }
        }
      }
    }

    /*
   * Sort the files heuristically to load the plugin jar containing plugin descriptors without extra ZipFile accesses
   * File name preference:
   * a) last order for files with resources in name, like resources_en.jar
   * b) last order for files that have -digit suffix is the name e.g. completion-ranking.jar is before json-2.8.0.jar or junit-m5.jar
   * c) jar with name close to plugin's directory name, e.g. kotlin-XXX.jar is before all-open-XXX.jar
   * d) shorter name, e.g. android.jar is before android-base-common.jar
   */
    private fun putMoreLikelyPluginJarsFirst(pluginDir: File, filesInLibUnderPluginDir: Array<File>) {
      val pluginDirName = pluginDir.name
      Arrays.parallelSort(filesInLibUnderPluginDir) { o1: File, o2: File ->
        val o2Name = o2.name
        val o1Name = o1.name
        val o2StartsWithResources = o2Name.startsWith("resources")
        val o1StartsWithResources = o1Name.startsWith("resources")
        if (o2StartsWithResources != o1StartsWithResources) {
          return@parallelSort if (o2StartsWithResources) -1 else 1
        }
        val o2IsVersioned = fileNameIsLikeVersionedLibraryName(o2Name)
        val o1IsVersioned = fileNameIsLikeVersionedLibraryName(o1Name)
        if (o2IsVersioned != o1IsVersioned) {
          return@parallelSort if (o2IsVersioned) -1 else 1
        }
        val o2StartsWithNeededName = o2Name.startsWith(pluginDirName, true)
        val o1StartsWithNeededName = o1Name.startsWith(pluginDirName, true)
        if (o2StartsWithNeededName != o1StartsWithNeededName) {
          return@parallelSort if (o2StartsWithNeededName) 1 else -1
        }
        o1Name.length - o2Name.length
      }
    }

    private fun fileNameIsLikeVersionedLibraryName(name: String): Boolean {
      val i = name.lastIndexOf('-')
      if (i == -1) return false
      if (i + 1 < name.length) {
        val c = name[i + 1]
        return if (Character.isDigit(c)) true else (c == 'm' || c == 'M') && i + 2 < name.length && Character.isDigit(name[i + 2])
      }
      return false
    }

    private fun toCanonicalPath(descriptorPath: String): String {
      return File(descriptorPath.toSystemIndependentName()).normalize().path
    }

    private fun getZipEntry(zipFile: ZipFile, entryPath: String): ZipEntry? {
      val independentPath = entryPath.toSystemIndependentName()
      val independentEntry = zipFile.getEntry(independentPath)
      if (independentEntry != null) {
        return independentEntry
      }
      val dependentPath = entryPath.replace('/', File.separatorChar)
      return if (dependentPath != independentPath) {
        zipFile.getEntry(dependentPath)
      } else null
    }

    private fun getIconFileName(iconTheme: IconTheme) = "pluginIcon${iconTheme.suffix}.svg"
  }

}