/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.fleet.bean.Barrel
import com.jetbrains.plugin.structure.fleet.bean.PluginDescriptor
import com.jetbrains.plugin.structure.fleet.problems.createIncorrectFleetPluginFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FleetPluginManager private constructor(private val extractDirectory: Path, val mockFilesContent: Boolean) : PluginManager<FleetPlugin> {
  companion object {
    const val DESCRIPTOR_NAME = "extension.json"

    private val LOG: Logger = LoggerFactory.getLogger(FleetPluginManager::class.java)

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get()),
      mockFilesContent: Boolean = false
    ): FleetPluginManager {
      extractDirectory.createDir()
      return FleetPluginManager(extractDirectory, mockFilesContent)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<FleetPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectFleetPluginFile(pluginFile.simpleName))
    }
  }


  private fun loadDescriptorFromZip(pluginFile: Path): PluginCreationResult<FleetPlugin> {
    val sizeLimit = Settings.FLEET_PLUGIN_SIZE_LIMIT.getAsLong()
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

  private fun loadPluginInfoFromDirectory(pluginDirectory: Path): PluginCreationResult<FleetPlugin> {
    val descriptorFile = pluginDirectory.resolve(DESCRIPTOR_NAME)
    if (!descriptorFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    val icons = loadIconFromDir(pluginDirectory)
    return createPlugin(descriptorFile.readText(), icons, pluginDirectory)
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

  private fun createPlugin(serializedDescriptor: String, icons: List<PluginIcon>, pluginDir: Path): PluginCreationResult<FleetPlugin> {
    try {
      val descriptor = Json.decodeFromString<PluginDescriptor>(serializedDescriptor)
      val beanValidationResult = validateFleetPluginBean(descriptor)

      val fileChecker = FileChecker(descriptor.id.name)
      val fr = descriptor.frontend?.parse(pluginDir, fileChecker)
      val ws = descriptor.workspace?.parse(pluginDir, fileChecker)
      beanValidationResult.addAll(fileChecker.problems)

      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      val plugin = with(descriptor) {
        FleetPlugin(
          pluginId = id.name,
          pluginVersion = version.version.value,
          depends = deps,
          frontend = fr,
          workspace = ws,
          pluginName = readableName,
          description = description,
          icons = icons,
          vendor = vendor,
        )
      }
      return PluginCreationSuccess(plugin, beanValidationResult)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor $DESCRIPTOR_NAME", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, e.localizedMessage))
    }
  }

  private fun Barrel.parse(pluginDir: Path, fileChecker: FileChecker): ParsedPluginPart {
    // format: "path_in_zip/filename-1.1.1.ext#hash12345"
    fun Collection<Barrel.Coordinates>.parse(pluginDir: Path): List<PluginFile> {
      val files = mutableListOf<PluginFile>()
      for (coord in this) {
        val relCoord = coord as? Barrel.Coordinates.Relative
        if (relCoord == null) {
          LOG.error("Only relative coordinates are expected in plugin distribution: skipping $coord")
          continue
        }

        val sha = relCoord.hash
        val filePath = relCoord.relPath
        val filename = filePath.substringAfterLast("/")
        if (mockFilesContent) {
          files.add(PluginFile(filename, sha, ByteArray(0)))
        } else {
          if (fileChecker.addFile(pluginDir, filePath)) {
            val file = pluginDir.resolve(filePath)
            val content = Files.readAllBytes(file)
            files.add(PluginFile(filename, sha, content))
          }
        }
      }
      return files
    }

    return ParsedPluginPart(
      modulePath.parse(pluginDir),
      classPath.parse(pluginDir),
      squashedAutomaticModules.map { it.parse(pluginDir) }.toSet(),
      modules
    )
  }
}


