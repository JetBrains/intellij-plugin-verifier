package com.jetbrains.structure.teamcity

import com.jetbrains.structure.plugin.PluginCreationFail
import com.jetbrains.structure.plugin.PluginCreationResult
import com.jetbrains.structure.plugin.PluginCreationSuccess
import com.jetbrains.structure.plugin.PluginProblem
import com.jetbrains.structure.problems.*
import com.jetbrains.structure.teamcity.beans.extractPluginBean
import com.jetbrains.structure.utils.FileUtil
import org.jdom2.input.JDOMParseException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

private val DESCRIPTOR_NAME = "teamcity-plugin.xml"

fun createTeamcityPlugin(pluginFile: File): PluginCreationResult<TeamcityPlugin> {
  if (!pluginFile.exists()) {
    throw IllegalArgumentException("Plugin file $pluginFile does not exist")
  }

  return if (FileUtil.isZip(pluginFile)) {
    try {
      loadDescriptorFromZip(ZipFile(pluginFile))
    } catch (e: IOException) {
      return PluginCreationFail(UnableToExtractZip(pluginFile))
    }
  } else if (pluginFile.isDirectory) {
    loadDescriptorFromDirectory(pluginFile)
  } else {
    PluginCreationFail(IncorrectPluginFile(pluginFile))
  }
}

private fun loadDescriptorFromZip(pluginFile: ZipFile): PluginCreationResult<TeamcityPlugin> {
  val descriptorEntry = pluginFile.getEntry(DESCRIPTOR_NAME) ?:
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  return loadDescriptorFromStream(pluginFile.getInputStream(descriptorEntry))
}

private fun loadDescriptorFromDirectory(pluginDirectory: File): PluginCreationResult<TeamcityPlugin> {
  val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
  if (!descriptorFile.exists()) {
    return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  }
  return loadDescriptorFromStream(descriptorFile.inputStream())
}

private fun loadDescriptorFromStream(inputStream: InputStream): PluginCreationResult<TeamcityPlugin> {
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
    return PluginCreationFail(UnableToReadDescriptor())
  }
}