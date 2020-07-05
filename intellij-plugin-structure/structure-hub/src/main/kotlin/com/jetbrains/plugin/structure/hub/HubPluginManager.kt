/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.hub

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.hub.bean.HubPluginManifest
import com.jetbrains.plugin.structure.hub.problems.HubIconInvalidUrl
import com.jetbrains.plugin.structure.hub.problems.createIncorrectHubPluginFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class HubPluginManager private constructor() : PluginManager<HubPlugin> {
  companion object {
    const val DESCRIPTOR_NAME = "manifest.json"

    private val LOG: Logger = LoggerFactory.getLogger(HubPluginManager::class.java)

    fun createManager(): HubPluginManager = HubPluginManager()
  }

  override fun createPlugin(pluginFile: File): PluginCreationResult<HubPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isDirectory -> loadPluginInfoFromDirectory(pluginFile)
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile.inputStream())
      else -> PluginCreationFail(createIncorrectHubPluginFile(pluginFile.name))
    }
  }

  override fun createPlugin(pluginFileContent: InputStream, pluginFileName: String) =
    loadDescriptorFromZip(pluginFileContent)


  private fun loadDescriptorFromZip(pluginFileContent: InputStream): PluginCreationResult<HubPlugin> {
    val sizeLimit = Settings.HUB_PLUGIN_SIZE_LIMIT.getAsLong()
    if (pluginFileContent.available() > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    }

    val extractDirectory = Settings.EXTRACT_DIRECTORY.getAsFile().toPath().createDir()
    val tempDirectory = Files.createTempDirectory(extractDirectory, "plugin_").toFile()
    return try {
      extractZip(pluginFileContent, tempDirectory, sizeLimit)
      loadPluginInfoFromDirectory(tempDirectory)
    } catch (e: DecompressorSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(e.sizeLimit))
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadPluginInfoFromDirectory(pluginDirectory: File): PluginCreationResult<HubPlugin> {
    val errors = validateHubPluginDirectory(pluginDirectory)
    if (errors != null) {
      return errors
    }
    val manifestFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (!manifestFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    val manifestContent = manifestFile.readText()
    val manifest = Json(JsonConfiguration.Stable.copy(isLenient = true, ignoreUnknownKeys = true))
      .parse(HubPluginManifest.serializer(), manifestContent)
    val iconFilePath = getIconFile(pluginDirectory.toPath(), manifest.iconUrl)
    return when {
      iconFilePath == null -> createPlugin(manifest, manifestContent, null)
      Files.exists(iconFilePath) -> createPlugin(
        manifest, manifestContent, PluginIcon(
        IconTheme.DEFAULT, Files.readAllBytes(iconFilePath), iconFilePath.fileName.toString()
      )
      )
      else -> PluginCreationFail(HubIconInvalidUrl(manifest.iconUrl))
    }
  }

  private fun createPlugin(
    descriptor: HubPluginManifest, manifestContent: String, icon: PluginIcon?
  ): PluginCreationResult<HubPlugin> {
    try {
      val beanValidationResult = validateHubPluginBean(descriptor)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }

      val vendorInfo = parseHubVendorInfo(descriptor.author ?: "")
      if (vendorInfo.vendor == null) return PluginCreationFail(PropertyNotSpecified("author"))
      val plugin = with(descriptor) {
        HubPlugin(
          pluginId = pluginId,
          pluginName = pluginName,
          pluginVersion = pluginVersion,
          url = url,
          description = description,
          dependencies = dependencies,
          products = products,
          vendor = vendorInfo.vendor,
          vendorEmail = vendorInfo.vendorEmail,
          vendorUrl = vendorInfo.vendorUrl,
          manifestContent = manifestContent,
          icons = if (icon != null) listOf(icon) else emptyList()
        )
      }
      return PluginCreationSuccess(plugin, beanValidationResult)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor $DESCRIPTOR_NAME", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, e.localizedMessage))
    }
  }

  private fun getIconFile(pluginDirectory: Path, iconUrl: String?): Path? {
    return when {
      iconUrl == null -> null
      iconUrl.contains("://") || iconUrl.startsWith("/") -> {
        LOG.warn("Unsupported widget iconUrl: '$iconUrl'")
        null
      }
      else -> {
        val iconPath = pluginDirectory.resolve(iconUrl).normalize()
        return if (iconPath.startsWith(pluginDirectory)) iconPath else null
      }
    }
  }
}