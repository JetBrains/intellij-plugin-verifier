/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBeanExtractor
import com.jetbrains.plugin.structure.teamcity.problems.createIncorrectTeamCityPluginFile
import org.slf4j.LoggerFactory
import org.xml.sax.SAXParseException
import java.io.File
import java.io.InputStream
import java.nio.file.Files

class TeamcityPluginManager private constructor(private val validateBean: Boolean) : PluginManager<TeamcityPlugin> {
  companion object {
    private const val DESCRIPTOR_NAME = "teamcity-plugin.xml"

    private val LOG = LoggerFactory.getLogger(TeamcityPluginManager::class.java)

    fun createManager(validateBean: Boolean = true): TeamcityPluginManager =
        TeamcityPluginManager(validateBean)

  }

  override fun createPlugin(pluginFile: File): PluginCreationResult<TeamcityPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isDirectory -> loadDescriptorFromDirectory(pluginFile)
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile.inputStream())
      else -> PluginCreationFail(createIncorrectTeamCityPluginFile(pluginFile.name))
    }
  }

  override fun createPlugin(pluginFileContent: InputStream, pluginFileName: String) =
    loadDescriptorFromZip(pluginFileContent)

  private fun loadDescriptorFromZip(pluginFileContent: InputStream): PluginCreationResult<TeamcityPlugin> {
    val sizeLimit = Settings.TEAM_CITY_PLUGIN_SIZE_LIMIT.getAsLong()
    if (pluginFileContent.available() > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    }
    val extractDirectory = Settings.EXTRACT_DIRECTORY.getAsFile().toPath().createDir()
    val tempDirectory = Files.createTempDirectory(extractDirectory, "plugin_").toFile()
    return try {
      extractZip(pluginFileContent, tempDirectory, sizeLimit)
      loadDescriptorFromDirectory(tempDirectory)
    } catch (e: DecompressorSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(e.sizeLimit))
    } catch (e: Exception) {
      return PluginCreationFail(UnableToExtractZip())
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadDescriptorFromDirectory(pluginDirectory: File): PluginCreationResult<TeamcityPlugin> {
    val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (descriptorFile.exists()) {
      return loadDescriptor(descriptorFile)
    }
    return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  }

  private fun loadDescriptor(descriptorFile: File): PluginCreationResult<TeamcityPlugin> {
    try {
      val bean = descriptorFile.inputStream().buffered().use {
        TeamcityPluginBeanExtractor.extractPluginBean(it)
      }

      if (!validateBean) return PluginCreationSuccess(bean.toPlugin(), emptyList())

      val beanValidationResult = validateTeamcityPluginBean(bean)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      return PluginCreationSuccess(bean.toPlugin(), beanValidationResult)
    } catch (e: SAXParseException) {
      val lineNumber = e.lineNumber
      val message = if (lineNumber != -1) "unexpected element on line $lineNumber" else "unexpected elements"
      return PluginCreationFail(UnexpectedDescriptorElements(message))
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor from ${descriptorFile.name}", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, e.localizedMessage))
    }
  }
}