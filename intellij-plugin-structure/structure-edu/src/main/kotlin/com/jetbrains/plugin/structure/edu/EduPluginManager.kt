/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.edu.bean.EduPluginDescriptor
import com.jetbrains.plugin.structure.edu.problems.createIncorrectEduPluginFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class EduPluginManager private constructor(private val extractDirectory: Path) : PluginManager<EduPlugin> {
  companion object {
    const val DESCRIPTOR_NAME = "course.json"
    const val COURSE_ICON_NAME = "courseIcon.svg"

    private val LOG: Logger = LoggerFactory.getLogger(EduPluginManager::class.java)

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get())
    ): EduPluginManager {
      extractDirectory.createDir()
      return EduPluginManager(extractDirectory)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<EduPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectEduPluginFile(pluginFile.simpleName))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: Path): PluginCreationResult<EduPlugin> {
    val sizeLimit = Settings.EDU_PLUGIN_SIZE_LIMIT.getAsLong()
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

  private fun loadPluginInfoFromDirectory(pluginDirectory: Path): PluginCreationResult<EduPlugin> {
    val descriptorFile = pluginDirectory.resolve(DESCRIPTOR_NAME)
    if (!descriptorFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    val descriptorContent = descriptorFile.readText()
    val mapper = jacksonObjectMapper()
    val descriptor = mapper.readValue(descriptorContent, EduPluginDescriptor::class.java)
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

  private fun createPlugin(descriptor: EduPluginDescriptor, icon: PluginIcon?): PluginCreationResult<EduPlugin> {
    try {
      val beanValidationResult = validateEduPluginBean(descriptor)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      val plugin = with(descriptor) {
        /*
          This is a workaround on complex, strange and outdated logic about xml id generation. Long story short:
            1. There was no xml id for edu plugins, so we need to generate it by our self. The original generation alg was
               "${this.title}_${this.vendor?.name}_$programmingLanguage".
            2. Then, it appears that authors should be able to change the `title`. To do so, edu team introduced
               `generated_edu_id` field in course json. The idea is to generate it the same way as described in (1) in
               the edu plugin and only after that allow author to change the `title`.
            3. Now, it appears that `programmingLanguage` could contain the programming language, and it's version divided
               by space. Edu also want to split `programmingLanguage` to `programmingLanguageId` and `programmingLanguageVersion`.
               Also, `programmingLanguageVersion` could be changed in future versions of the course, which means it
               could also affect xml id generation.

          So, the plan is:
            1. For older versions of the course structure (with `programmingLanguage`) we will get rid of the programming
               language version in xml id. I'll also remove the version from xml id in the database. From now on
               `programmingLanguage` is deprecated with estimated period of 6 edu plugin releases (half of a year).
            2. For newer versions of the course structure (with `programmingLanguageId` and `programmingLanguageVersion`)
               we will use only `programmingLanguageId` in xml id.
            3. We also decided to remove the part with generation xml id and to use `generated_edu_id` instead for every
               plugin with an estimated period of 6 edu plugin releases.
         */
        val xmlIdSuffix = this.programmingLanguageId ?: this.programmingLanguage?.substringBefore(" ")
        val pluginId = this.pluginId ?: "${this.title}_${this.vendor?.name}_$xmlIdSuffix"

        EduPlugin(
          pluginName = this.title,
          description = this.summary,
          vendor = this.vendor?.name,
          vendorUrl = this.vendor?.vendorUrl,
          vendorEmail = this.vendor?.vendorEmail,
          descriptorVersion = this.descriptorVersion,
          pluginVersion = this.pluginVersion,
          language = this.language,
          programmingLanguage = programmingLanguage,
          programmingLanguageId = programmingLanguageId,
          programmingLanguageVersion = this.programmingLanguageVersion,
          environment = this.environment,
          isPrivate = this.isPrivate ?: false,
          eduStat = EduStat.fromDescriptor(this),
          icons = if (icon != null) listOf(icon) else emptyList(),
          pluginId = pluginId
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