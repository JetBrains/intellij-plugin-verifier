/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.IconTheme
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.PluginManager
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.plugin.parseThirdPartyDependenciesByPath
import com.jetbrains.plugin.structure.base.problems.IncorrectZipOrJarFile
import com.jetbrains.plugin.structure.base.problems.MultiplePluginDescriptors
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.base.problems.isInvalidDescriptorProblem
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.getShortExceptionMessage
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.pluginSize
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.plugin.structure.base.utils.withPathSeparatorOf
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor.extractPlugin
import com.jetbrains.plugin.structure.intellij.plugin.Module.FileBasedModule
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createInvalidPlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.Companion.createPlugin
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.JarArchiveCannotBeOpenException
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import com.jetbrains.plugin.structure.jar.PluginJar
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.jdom2.Document
import org.jdom2.input.JDOMParseException
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
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

  private val optionalDependencyResolver = OptionalDependencyResolver(this::loadPluginInfoFromJarOrDirectory)

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

  private fun loadModuleInfoFromJarFile(
    jarFile: Path,
    descriptorPath: String,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver,
  ): PluginCreator {
    return try {
      PluginJar(jarFile, fileSystemProvider).use { jar ->
        when (val descriptor = jar.getPluginDescriptor(descriptorPath)) {
          is Found -> {
            try {
              val descriptorXml = descriptor.loadXml()
              createPlugin(
                jarFile.simpleName,
                descriptorPath,
                parentPlugin = null,
                validateDescriptor = false,
                descriptorXml,
                descriptor.path, resourceResolver, problemResolver
              )
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

  private fun loadModuleFromDescriptorResource(
    moduleId: String,
    descriptorResource: DescriptorResource,
    parentPlugin: PluginCreator? = null,
    resourceResolver: ResourceResolver
  ): PluginCreator {
    return descriptorResource.inputStream.use {
      try {
        val problemResolver = AnyProblemToWarningPluginCreationResultResolver
        val descriptorXml = JDOMUtil.loadDocument(it)
        createPlugin(
          descriptorResource,
          parentPlugin,
          descriptorXml,
          resourceResolver,
          problemResolver
        ).also {
          logPluginCreationWarnings(moduleId, it)
        }
      } catch (e: IOException) {
        with(descriptorResource) {
          LOG.warn("Unable to read descriptor stream (source: '$uri')", e)
          val problem = UnableToReadDescriptor(fileName, e.localizedMessage)
          createInvalidPlugin(artifactFileName, fileName, problem)
        }
      }
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
      optionalDependencyResolver.resolveOptionalDependencies(pluginCreator, pluginFile, resourceResolver, problemResolver)
    }
  }

  private fun resolveContentModules(pluginFile: Path, currentPlugin: PluginCreator, resourceResolver: ResourceResolver, problemResolver: PluginCreationResultResolver) {
    if (currentPlugin.isSuccess) {
      val contentModules = currentPlugin.contentModules
      for (module in contentModules) {
        when (module) {
          is FileBasedModule -> {
            val configFile = module.configFile
            val moduleCreator = loadPluginInfoFromJarOrDirectory(
              pluginFile,
              configFile,
              false,
              resourceResolver,
              currentPlugin,
              problemResolver
            )
            currentPlugin.addModuleDescriptor(module.name, module.loadingRule, configFile, moduleCreator)
          }

          is InlineModule -> {
            val moduleDescriptorResource = getDescriptorResource(module, pluginFile, currentPlugin.descriptorPath)
            val moduleCreator =
              loadModuleFromDescriptorResource(module.name, moduleDescriptorResource, currentPlugin, resourceResolver)
            currentPlugin.addModuleDescriptor(module, module.loadingRule, moduleDescriptorResource, moduleCreator)
          }
        }
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
          getPluginCreator(extractedFile, descriptorPath, validateDescriptor, resourceResolver, problemResolver)
        } else {
          getInvalidPluginFileCreator(pluginFile.simpleName, descriptorPath)
        }
      }

      is ExtractorResult.Fail -> createInvalidPlugin(pluginFile.simpleName, descriptorPath, extractorResult.pluginProblem)
    }
  }

  override fun createPlugin(pluginFile: Path) = createPlugin(pluginFile, true)

  @Throws(PluginFileNotFoundException::class)
  fun createPlugin(
    pluginFile: Path,
    validateDescriptor: Boolean,
    descriptorPath: String = PLUGIN_XML,
    problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, validateDescriptor, descriptorPath, problemResolver)
    return pluginCreator.pluginCreationResult
  }

  @Throws(PluginFileNotFoundException::class)
  fun createBundledPlugin(
    pluginFile: Path,
    ideVersion: IdeVersion,
    descriptorPath: String,
    problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
    fallbackPluginId: String? = null,
  ): PluginCreationResult<IdePlugin> {
    val pluginCreator = getPluginCreatorWithResult(pluginFile, false, descriptorPath, problemResolver)
    pluginCreator.setPluginVersion(ideVersion.asStringWithoutProductCode())
    fallbackPluginId?.let { pluginCreator.setPluginIdIfNull(it) }
    return pluginCreator.pluginCreationResult
  }

  fun createBundledModule(
    pluginFile: Path,
    ideVersion: IdeVersion,
    descriptorPath: String,
    problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
  ): PluginCreationResult<IdePlugin> {
    return loadModuleInfoFromJarFile(pluginFile, descriptorPath, myResourceResolver, problemResolver).apply {
      setPluginVersion(ideVersion.asStringWithoutProductCode())
      setOriginalFile(pluginFile)
    }.pluginCreationResult
  }

  @Throws(PluginFileNotFoundException::class)
  private fun getPluginCreatorWithResult(
    pluginFile: Path,
    validateDescriptor: Boolean,
    descriptorPath: String,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    if (!pluginFile.exists()) { throw PluginFileNotFoundException(pluginFile) }
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
        pluginCreator = getPluginCreator(pluginFile, descriptorPath, validateDescriptor, myResourceResolver, problemResolver)
      } else {
        pluginCreator = getInvalidPluginFileCreator(pluginFile.simpleName, descriptorPath)
      }
      pluginCreator.setOriginalFile(pluginFile)
    }.let { pluginCreationDuration -> pluginCreator.setTelemetry(pluginFile, pluginCreationDuration)}
    return pluginCreator
  }

  private fun getPluginCreator(
    pluginFile: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator {
    val pluginCreator = loadPluginInfoFromJarOrDirectory(pluginFile, descriptorPath, validateDescriptor, resourceResolver, null, problemResolver)
    resolveOptionalDependencies(pluginFile, pluginCreator, myResourceResolver, problemResolver)
    resolveContentModules(pluginFile, pluginCreator, myResourceResolver, problemResolver)

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

  private fun getDescriptorResource(module: InlineModule, pluginFile: Path, descriptorPath: String): DescriptorResource {
    // TODO descriptor path is not relative to the pluginFile JAR. See MP-7224
    val parentUriStr = if (pluginFile.isJar()) {
      "jar:" + pluginFile.toUri().toString() + "!" + descriptorPath.toSystemIndependentName()
    } else {
      pluginFile.toUri().toString() + "/" + descriptorPath.toSystemIndependentName()
    }
    val uriStr = parentUriStr + "#modules/" + module.name
    return DescriptorResource(module.textContent.byteInputStream(), URI(uriStr), URI(parentUriStr))
  }

  private fun logPluginCreationWarnings(pluginId: String, pluginCreator: PluginCreator) {
    val pluginCreationResult = pluginCreator.pluginCreationResult
    if (LOG.isDebugEnabled && pluginCreationResult is PluginCreationSuccess) {
      val warningMessage = pluginCreationResult.warnings.joinToString("\n") {
        it.message
      }
      LOG.debug("Plugin or module '$pluginId' has plugin problems: $warningMessage")
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