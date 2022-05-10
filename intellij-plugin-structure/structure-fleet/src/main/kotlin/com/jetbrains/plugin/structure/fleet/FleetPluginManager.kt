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
import com.jetbrains.plugin.structure.fleet.problems.createIncorrectFleetPluginFile
import fleet.bundles.BundleSpec
import fleet.bundles.decodeFromString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class FleetPluginManager private constructor(private val extractDirectory: Path) : PluginManager<FleetPlugin> {
  companion object {
    const val DESCRIPTOR_NAME = "extension.json"

    private val LOG: Logger = LoggerFactory.getLogger(FleetPluginManager::class.java)

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get()),
    ): FleetPluginManager {
      extractDirectory.createDir()
      return FleetPluginManager(extractDirectory)
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
      val bundleSpec = BundleSpec.decodeFromString(serializedDescriptor)
      val beanValidationResult = validateFleetPluginBean(bundleSpec)

      val fileChecker = FileChecker(bundleSpec.bundleId.name.name)
      beanValidationResult.addAll(fileChecker.problems)

      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      val plugin = with(bundleSpec) {
        val files = bundleSpec.getFiles(pluginDir, fileChecker)
        FleetPlugin(
          pluginId = bundleId.name.name,
          pluginVersion = bundleId.version.version.value,
          pluginName = readableName,
          description = description,
          icons = icons,
          vendor = vendor,
          descriptorFileName = DESCRIPTOR_NAME,
          files = files
        )
      }
      return PluginCreationSuccess(plugin, beanValidationResult)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor $DESCRIPTOR_NAME", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, "Bad descriptor format. Descriptor text: $serializedDescriptor" + "\n" + e.localizedMessage))
    }
  }

  private fun BundleSpec.getFiles(pluginDir: Path, fileChecker: FileChecker): List<PluginFile> {
    //todo [MM]: extract files from descriptor or filter them by layout
    return Files.walk(pluginDir).use { pathStream ->
      pathStream.filter { !it.isDirectory }.map {
        val filePath = it.fileName.toString()
        val filename = filePath.substringAfterLast("/")

        if (fileChecker.addFile(pluginDir, filePath)) {
          val file = pluginDir.resolve(filePath)
          val content = Files.readAllBytes(file)
          PluginFile(filename, content)
        } else null
      }.toList().filterNotNull()
    }
  }
}

