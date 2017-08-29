package com.jetbrains.structure.teamcity

import com.jetbrains.structure.plugin.PluginCreationFail
import com.jetbrains.structure.plugin.PluginCreationResult
import com.jetbrains.structure.plugin.PluginCreationSuccess
import com.jetbrains.structure.plugin.PluginProblem
import com.jetbrains.structure.problems.*
import com.jetbrains.structure.teamcity.beans.extractPluginBean
import com.jetbrains.structure.utils.FileUtil
import com.jetbrains.structure.utils.ZipUtil
import org.jdom2.input.JDOMParseException
import java.io.File

private val DESCRIPTOR_NAME = "teamcity-plugin.xml"

fun createTeamcityPlugin(pluginFile: File): PluginCreationResult<TeamcityPlugin> {
  if (!pluginFile.exists()) {
    throw IllegalArgumentException("Plugin file $pluginFile does not exist")
  }

  return if (FileUtil.isZip(pluginFile)) {
    try {
      ZipUtil.withExtractedZipArchive(pluginFile) { extractedArchive: File ->
        loadDescriptorFromDirectory(extractedArchive)
      }
    } catch (e: Throwable){
      return PluginCreationFail(UnableToExtractZip(pluginFile))
    }
  } else if (pluginFile.isDirectory) {
    loadDescriptorFromDirectory(pluginFile)
  } else {
    PluginCreationFail(IncorrectPluginFile(pluginFile))
  }
}

private fun loadDescriptorFromDirectory(pluginDirectory: File): PluginCreationResult<TeamcityPlugin> {
  try {
    val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (!descriptorFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    val bean = extractPluginBean(descriptorFile)

    val beanValidationResult = validateTeamcityPluginBean(bean)
    if(beanValidationResult.any { it.level == PluginProblem.Level.ERROR }){
      return PluginCreationFail(beanValidationResult)
    }

    return PluginCreationSuccess(bean.toPlugin(), beanValidationResult)
  } catch (e: JDOMParseException) {
    val lineNumber = e.lineNumber
    val message = if(lineNumber != -1) "unexpected element on line " + lineNumber else "unexpected elements"
    return PluginCreationFail(UnexpectedDescriptorElements(message))
  } catch (e: Exception){
    return PluginCreationFail(UnableToReadDescriptor())
  }
}