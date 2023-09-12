/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.toolbox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class ToolboxPluginManager private constructor(private val extractDirectory: Path) : PluginManager<ToolboxPlugin> {
  companion object {
    const val DESCRIPTOR_NAME = "extension.json"
    const val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

    private val LOG: Logger = LoggerFactory.getLogger(ToolboxPluginManager::class.java)

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get()),
    ): ToolboxPluginManager {
      extractDirectory.createDir()
      return ToolboxPluginManager(extractDirectory)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<ToolboxPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectToolboxPluginFile(pluginFile.simpleName))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: Path): PluginCreationResult<ToolboxPlugin> {
    val sizeLimit = Settings.TOOLBOX_PLUGIN_SIZE_LIMIT.getAsLong()
    if (Files.size(pluginFile) > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    }
    val tempDirectory = Files.createTempDirectory(extractDirectory, "plugin_")
    return try {
      extractZip(pluginFile, tempDirectory, sizeLimit)
      loadPluginInfoFromDirectory(tempDirectory)
    } catch (e: DecompressorSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(e.sizeLimit))
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadPluginInfoFromDirectory(pluginDirectory: Path): PluginCreationResult<ToolboxPlugin> {
    val descriptorFile = pluginDirectory.resolve(DESCRIPTOR_NAME)
    if (!descriptorFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    return createPlugin(descriptorFile.readText(), pluginDirectory)
  }

  private fun loadIconFromDir(pluginDirectory: Path): List<PluginIcon> =
    IconTheme.values().mapNotNull { theme ->
      val iconEntryName = getIconFileName(theme)
      val iconPath = pluginDirectory.resolve(iconEntryName)
      if (iconPath.exists()) {
        PluginIcon(theme, iconPath.readBytes(), iconEntryName)
      } else {
        null
      }
    }

  private fun getIconFileName(iconTheme: IconTheme) = "pluginIcon${iconTheme.suffix}.svg"

  private fun createPlugin(serializedDescriptor: String, pluginDir: Path): PluginCreationResult<ToolboxPlugin> {
    try {
      val descriptor = ToolboxPluginDescriptor.parse(serializedDescriptor)
      val problems = descriptor.validate()

      val icons = loadIconFromDir(pluginDir)
      val iconNames = icons.map { it.fileName }.toSet()

      val fileChecker = FileChecker()
      val files = Files.list(pluginDir).toList().mapNotNull { file ->
        val fileName = file.fileName.toString()
        if (file.isFile && fileName !in iconNames && fileChecker.addFile(file)) {
          PluginFile(fileName, Files.readAllBytes(file))
        }
        else {
          null
        }
      }
      if (problems.any { it.level == PluginProblem.Level.ERROR } || fileChecker.problems.isNotEmpty()) {
        return PluginCreationFail(problems + fileChecker.problems)
      }

      val plugin = ToolboxPlugin(
        pluginId = requireNotNull(descriptor.id),
        pluginVersion = requireNotNull(descriptor.version),
        compatibleVersionRange = requireNotNull(descriptor.compatibleVersionRange),
        pluginName = descriptor.meta?.name,
        description = descriptor.meta?.description,
        vendor = descriptor.meta?.vendor,
        icons = icons,
        descriptorFileName = DESCRIPTOR_NAME,
        thirdPartyDependencies = parseThirdPartyDependenciesByPath(pluginDir.resolve(THIRD_PARTY_LIBRARIES_FILE_NAME)),
        files = files
      )
      return PluginCreationSuccess(plugin, problems)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor $DESCRIPTOR_NAME", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, "Bad descriptor format. Descriptor text: $serializedDescriptor" + "\n" + e.localizedMessage))
    }
  }

  private fun parseThirdPartyDependenciesByPath(path: Path): List<ThirdPartyDependency> {
    return if (path.exists()) {
      jacksonObjectMapper().readValue(Files.readAllBytes(path))
    }
    else {
      emptyList()
    }
  }
}

fun createIncorrectToolboxPluginFile(fileName: String): PluginFileError =
  IncorrectPluginFile(fileName, ".zip archive.")
