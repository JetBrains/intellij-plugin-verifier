package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.teamcity.beans.extractPluginBean
import com.jetbrains.plugin.structure.teamcity.problems.createIncorrectTeamCityPluginFile
import org.apache.commons.io.FileUtils
import org.jdom2.input.JDOMParseException
import org.slf4j.LoggerFactory
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
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectTeamCityPluginFile(pluginFile.name))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: File): PluginCreationResult<TeamcityPlugin> {
    val sizeLimit = Settings.TEAM_CITY_PLUGIN_SIZE_LIMIT.getAsLong()
    if (FileUtils.sizeOf(pluginFile) > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    }
    val tempDirectory = Files.createTempDirectory(Settings.EXTRACT_DIRECTORY.getAsFile().toPath(), pluginFile.nameWithoutExtension).toFile()
    return try {
      pluginFile.extractTo(tempDirectory, sizeLimit)
      loadDescriptorFromDirectory(tempDirectory)
    } catch (e: ArchiveSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
    } catch (e: Exception) {
      return PluginCreationFail(UnableToExtractZip())
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadDescriptorFromDirectory(pluginDirectory: File): PluginCreationResult<TeamcityPlugin> {
    val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (descriptorFile.exists()) {
      return descriptorFile.inputStream().use { loadDescriptorFromStream(descriptorFile.path, it) }
    }
    return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  }

  private fun loadDescriptorFromStream(streamName: String, inputStream: InputStream): PluginCreationResult<TeamcityPlugin> {
    try {
      val bean = extractPluginBean(inputStream)

      if (!validateBean) return PluginCreationSuccess(bean.toPlugin(), emptyList())

      val beanValidationResult = validateTeamcityPluginBean(bean)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      return PluginCreationSuccess(bean.toPlugin(), beanValidationResult)
    } catch (e: JDOMParseException) {
      val lineNumber = e.lineNumber
      val message = if (lineNumber != -1) "unexpected element on line $lineNumber" else "unexpected elements"
      return PluginCreationFail(UnexpectedDescriptorElements(message))
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor from $streamName", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, e.localizedMessage))
    }
  }
}