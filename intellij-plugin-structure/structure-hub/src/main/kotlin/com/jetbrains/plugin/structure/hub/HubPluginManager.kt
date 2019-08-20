package com.jetbrains.plugin.structure.hub

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.hub.problems.HubZipFileTooLargeError
import com.jetbrains.plugin.structure.hub.problems.createIncorrectHubPluginFile
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

class HubPluginManager private constructor() : PluginManager<HubPlugin> {
  companion object {
    private const val DESCRIPTOR_NAME = "manifest.json"

    private val LOG: Logger = LoggerFactory.getLogger(HubPluginManager::class.java)

    fun createManager(): HubPluginManager = HubPluginManager()
  }

  override fun createPlugin(pluginFile: File): PluginCreationResult<HubPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isDirectory -> loadDescriptorFromDirectory(pluginFile)
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectHubPluginFile(pluginFile.name))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: File): PluginCreationResult<HubPlugin> {
    val sizeLimit = Settings.HUB_PLUGIN_SIZE_LIMIT.getAsLong()
    if (FileUtils.sizeOf(pluginFile) > sizeLimit) {
      return PluginCreationFail(HubZipFileTooLargeError())
    }

    val tempDirectory = Files.createTempDirectory(Settings.EXTRACT_DIRECTORY.getAsFile().toPath(), pluginFile.nameWithoutExtension).toFile()
    return try {
      pluginFile.extractTo(tempDirectory, sizeLimit)
      loadDescriptorFromDirectory(tempDirectory)
    } catch (e: ArchiveSizeLimitExceededException) {
      return PluginCreationFail(HubZipFileTooLargeError())
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun loadDescriptorFromDirectory(pluginDirectory: File): PluginCreationResult<HubPlugin> {
    val errors = validateHubPluginDirectory(pluginDirectory)
    if (errors != null) {
      return errors
    }
    val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (descriptorFile.exists()) {
      return loadDescriptorFromStream(descriptorFile)
    }
    return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  }

  private fun loadDescriptorFromStream(descriptorFile: File): PluginCreationResult<HubPlugin> {
    try {
      val manifestContent = descriptorFile.readText()
      val descriptor = jacksonObjectMapper().readValue(manifestContent, HubPlugin::class.java)
      descriptor.manifestContent = manifestContent
      val vendorInfo = parseHubVendorInfo(descriptor.author)
      descriptor.apply {
        vendor = vendorInfo.vendor
        vendorEmail = vendorInfo.vendorEmail
        vendorUrl = vendorInfo.vendorUrl
      }
      val beanValidationResult = validateHubPluginBean(descriptor)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      return PluginCreationSuccess(descriptor, beanValidationResult)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor $DESCRIPTOR_NAME", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, e.localizedMessage))
    }
  }
}