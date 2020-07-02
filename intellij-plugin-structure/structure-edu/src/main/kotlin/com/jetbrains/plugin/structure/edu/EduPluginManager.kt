/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.edu.bean.EduPluginDescriptor
import com.jetbrains.plugin.structure.edu.problems.createIncorrectEduPluginFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

class EduPluginManager private constructor() : PluginManager<EduPlugin> {
  companion object {
    const val DESCRIPTOR_NAME = "course.json"
    const val COURSE_ICON_NAME = "courseIcon.svg"

    private val LOG: Logger = LoggerFactory.getLogger(EduPluginManager::class.java)

    fun createManager(): EduPluginManager = EduPluginManager()
  }

  override fun createPlugin(pluginFile: File): PluginCreationResult<EduPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectEduPluginFile(pluginFile.name))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: File): PluginCreationResult<EduPlugin> {
    val sizeLimit = Settings.EDU_PLUGIN_SIZE_LIMIT.getAsLong()
    if (FileUtils.sizeOf(pluginFile) > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    }

    val extractDirectory = Settings.EXTRACT_DIRECTORY.getAsFile().toPath().createDir()
    val tempDirectory = Files.createTempDirectory(extractDirectory, pluginFile.nameWithoutExtension).toFile()
    return try {
      pluginFile.extractTo(tempDirectory, sizeLimit)
      loadPluginInfoFromDirectory(tempDirectory)
    } catch (e: DecompressorSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(e.sizeLimit))
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadPluginInfoFromDirectory(pluginDirectory: File): PluginCreationResult<EduPlugin> {
    val errors = validateEduPluginDirectory(pluginDirectory)
    if (errors != null) return errors
    val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (!descriptorFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    val descriptorContent = descriptorFile.readText()
    val descriptor = Json(JsonConfiguration.Stable.copy(isLenient = true, ignoreUnknownKeys = true))
      .parse(EduPluginDescriptor.serializer(), descriptorContent)
    val icon = loadIconFromDir(pluginDirectory)
    return createPlugin(descriptor, icon)
  }

  private fun loadIconFromDir(pluginDirectory: File): PluginIcon? {
    val iconFile = File(pluginDirectory, COURSE_ICON_NAME)
    if (iconFile.exists()) {
      val iconContent = Files.readAllBytes(iconFile.toPath())
      return PluginIcon(IconTheme.DEFAULT, iconContent, iconFile.name)
    }
    return null
  }

  private fun createPlugin(descriptor: EduPluginDescriptor, icon: PluginIcon?): PluginCreationResult<EduPlugin> {
    try {
      val beanValidationResult = validateEduPluginBean(descriptor)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      val plugin = with(descriptor) {
        EduPlugin(
          pluginName = this.title,
          description = this.summary,
          vendor = this.vendor?.name,
          vendorUrl = this.vendor?.vendorUrl,
          vendorEmail = this.vendor?.vendorEmail,
          language = this.language,
          programmingLanguage = this.programmingLanguage,
          eduPluginVersion = this.eduPluginVersion,
          items = this.items?.map { it.title } ?: emptyList(),
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