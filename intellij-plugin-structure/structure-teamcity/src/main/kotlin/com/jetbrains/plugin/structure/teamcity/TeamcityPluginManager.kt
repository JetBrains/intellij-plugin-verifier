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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TeamcityPluginManager private constructor(
  private val extractDirectory: Path,
  private val validateBean: Boolean
) : PluginManager<TeamcityPlugin> {
  companion object {
    const val DESCRIPTOR_NAME = "teamcity-plugin.xml"
    const val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

    private val LOG = LoggerFactory.getLogger(TeamcityPluginManager::class.java)

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get()),
      validateBean: Boolean = true
    ): TeamcityPluginManager {
      extractDirectory.createDir()
      return TeamcityPluginManager(extractDirectory, validateBean)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<TeamcityPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isDirectory -> loadDescriptorFromDirectory(pluginFile)
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectTeamCityPluginFile(pluginFile.simpleName))
    }
  }


  private fun loadDescriptorFromZip(pluginFile: Path): PluginCreationResult<TeamcityPlugin> {
    val sizeLimit = Settings.TEAM_CITY_PLUGIN_SIZE_LIMIT.getAsLong()
    if (Files.size(pluginFile) > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    }
    val tempDirectory = Files.createTempDirectory(extractDirectory, "plugin_")
    return try {
      extractZip(pluginFile, tempDirectory, sizeLimit)
      loadDescriptorFromDirectory(tempDirectory)
    } catch (e: DecompressorSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(e.sizeLimit))
    } catch (e: Exception) {
      return PluginCreationFail(UnableToExtractZip())
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadDescriptorFromDirectory(pluginDirectory: Path): PluginCreationResult<TeamcityPlugin> {
    val descriptorFile = pluginDirectory.resolve(DESCRIPTOR_NAME)
    val dependenciesFile = pluginDirectory.resolve(THIRD_PARTY_LIBRARIES_FILE_NAME)
    val dependencies = parseThirdPartyDependenciesByPath(dependenciesFile)
    if (descriptorFile.exists()) {
      return loadDescriptor(descriptorFile, dependencies)
    }
    return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  }

  private fun loadDescriptor(descriptorFile: Path, dependencies: List<ThirdPartyDependency>): PluginCreationResult<TeamcityPlugin> {
    try {
      val bean = Files.newInputStream(descriptorFile).buffered().use {
        TeamcityPluginBeanExtractor.extractPluginBean(it)
      }

      if (!validateBean) return PluginCreationSuccess(bean.toPlugin(dependencies), emptyList())

      val beanValidationResult = validateTeamcityPluginBean(bean)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      return PluginCreationSuccess(bean.toPlugin(dependencies), beanValidationResult)
    } catch (e: SAXParseException) {
      return PluginCreationFail(UnexpectedDescriptorElements(e.lineNumber))
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor from ${descriptorFile.simpleName}", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, e.localizedMessage))
    }
  }
}