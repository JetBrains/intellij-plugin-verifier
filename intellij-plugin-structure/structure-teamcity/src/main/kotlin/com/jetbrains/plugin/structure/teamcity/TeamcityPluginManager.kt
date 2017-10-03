package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.logging.Logger
import com.jetbrains.plugin.structure.base.logging.LoggerFactory.createDefaultLogger
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.teamcity.beans.extractPluginBean
import org.jdom2.input.JDOMParseException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

class TeamcityPluginManager private constructor(private val logger: Logger) : PluginManager<TeamcityPlugin> {
  companion object {
    private val DESCRIPTOR_NAME = "teamcity-plugin.xml"

    fun createManager(): TeamcityPluginManager =
        createManager(createDefaultLogger(TeamcityPluginManager::class.java))

    fun createManager(logger: Logger): TeamcityPluginManager = TeamcityPluginManager(logger)
  }

  override fun createPlugin(pluginFile: File): PluginCreationResult<TeamcityPlugin> {
    if (!pluginFile.exists()) {
      throw IllegalArgumentException("Plugin file $pluginFile does not exist")
    }
    return when {
      pluginFile.isDirectory -> loadDescriptorFromDirectory(pluginFile)
      FileUtil.isZip(pluginFile) -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(IncorrectPluginFile(pluginFile))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: File): PluginCreationResult<TeamcityPlugin> = try {
    loadDescriptorFromZip(ZipFile(pluginFile))
  } catch (e: IOException) {
    logger.info("Unable to extract plugin zip: $pluginFile", e)
    PluginCreationFail(UnableToExtractZip(pluginFile))
  }

  private fun loadDescriptorFromZip(pluginFile: ZipFile): PluginCreationResult<TeamcityPlugin> {
    pluginFile.use {
      val descriptorEntry = pluginFile.getEntry(DESCRIPTOR_NAME) ?:
          return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
      return loadDescriptorFromStream(pluginFile.name, pluginFile.getInputStream(descriptorEntry))
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
      val beanValidationResult = validateTeamcityPluginBean(bean)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      return PluginCreationSuccess(bean.toPlugin(), beanValidationResult)
    } catch (e: JDOMParseException) {
      val lineNumber = e.lineNumber
      val message = if (lineNumber != -1) "unexpected element on line " + lineNumber else "unexpected elements"
      return PluginCreationFail(UnexpectedDescriptorElements(message))
    } catch (e: Exception) {
      logger.info("Unable to read plugin descriptor from $streamName", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME))
    }
  }
}