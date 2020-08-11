/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.ktor.bean.KtorFeatureDescriptor
import com.jetbrains.plugin.structure.ktor.problems.createIncorrectKtorFeatureFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class KtorFeaturePluginManager private constructor(private val extractDirectory: Path) : PluginManager<KtorFeature> {
  companion object {
    const val DESCRIPTOR_NAME = "descriptor.json"
    const val COURSE_ICON_NAME = "featureIcon.svg"

    private val LOG: Logger = LoggerFactory.getLogger(KtorFeaturePluginManager::class.java)

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get())
    ): KtorFeaturePluginManager {
      extractDirectory.createDir()
      return KtorFeaturePluginManager(extractDirectory)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<KtorFeature> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectKtorFeatureFile(pluginFile.simpleName))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: Path): PluginCreationResult<KtorFeature> {
    val sizeLimit = Settings.KTOR_FEATURE_SIZE_LIMIT.getAsLong()
    if (Files.size(pluginFile) > sizeLimit) {
      return PluginCreationFail(UnableToExtractZip())
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

  private fun loadPluginInfoFromDirectory(pluginDirectory: Path): PluginCreationResult<KtorFeature> {
    val descriptorFile = pluginDirectory.resolve(DESCRIPTOR_NAME)
    if (!descriptorFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    val descriptorContent = descriptorFile.readText()
    val descriptor = Json(JsonConfiguration.Stable.copy(isLenient = true, ignoreUnknownKeys = true))
      .parse(KtorFeatureDescriptor.serializer(), descriptorContent)
    val icon = loadIconFromDir(pluginDirectory)
    return createPlugin(descriptor, icon)
  }

  private fun loadIconFromDir(pluginDirectory: Path): PluginIcon? {
    val iconFile = pluginDirectory.resolve(COURSE_ICON_NAME)
    if (iconFile.exists()) {
      val iconContent = Files.readAllBytes(iconFile)
      return PluginIcon(IconTheme.DEFAULT, iconContent, iconFile.fileName.toString())
    }
    return null
  }

  private fun createPlugin(descriptor: KtorFeatureDescriptor, icon: PluginIcon?): PluginCreationResult<KtorFeature> {
    try {
      val beanValidationResult = validateKtorPluginBean(descriptor)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      val plugin = with(descriptor) {
        KtorFeature(
          pluginName = this.pluginName,
          description = this.description,
          copyright = this.copyright,
          vendor = this.vendor?.name,
          vendorUrl = this.vendor?.vendorUrl,
          vendorEmail = this.vendor?.vendorEmail,
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
}