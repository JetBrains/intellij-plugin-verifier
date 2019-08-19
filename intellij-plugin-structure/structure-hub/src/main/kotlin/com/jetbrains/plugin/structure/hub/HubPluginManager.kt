package com.jetbrains.plugin.structure.hub

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.hub.problems.createIncorrectHubPluginFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

class HubPluginManager private constructor(private val validateBean: Boolean) : PluginManager<HubPlugin> {
  companion object {
    private const val DESCRIPTOR_NAME = "manifest.json"

    private val LOG: Logger = LoggerFactory.getLogger(HubPluginManager::class.java)

    fun createManager(validateBean: Boolean = true): HubPluginManager =
        HubPluginManager(validateBean)
  }

  override fun createPlugin(pluginFile: File): PluginCreationResult<HubPlugin> {
    if (!pluginFile.exists()) {
      throw IllegalArgumentException("Plugin file $pluginFile does not exist")
    }
    return when {
      pluginFile.isDirectory -> loadDescriptorFromDirectory(pluginFile)
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectHubPluginFile(pluginFile.name))
    }
  }

  private fun loadDescriptorFromZip(pluginFile: File): PluginCreationResult<HubPlugin> = try {
    loadDescriptorFromZip(ZipFile(pluginFile))
  } catch (e: IOException) {
    LOG.info("Unable to extract plugin zip: $pluginFile", e)
    PluginCreationFail(UnableToExtractZip())
  }

  private fun loadDescriptorFromZip(pluginFile: ZipFile): PluginCreationResult<HubPlugin> {
    val errors = validateHubZipFile(pluginFile)
    if (errors != null) return errors
    pluginFile.use {
      val descriptorEntry = pluginFile.getEntry(DESCRIPTOR_NAME)
          ?: return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
      return loadDescriptorFromStream(pluginFile.name, pluginFile.getInputStream(descriptorEntry))
    }
  }

  private fun loadDescriptorFromDirectory(pluginDirectory: File): PluginCreationResult<HubPlugin> {
    val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (descriptorFile.exists()) {
      return descriptorFile.inputStream().use { loadDescriptorFromStream(descriptorFile.path, it) }
    }
    return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  }

  private fun loadDescriptorFromStream(streamName: String, inputStream: InputStream): PluginCreationResult<HubPlugin> {
    try {
      val descriptor = jacksonObjectMapper()
          .readValue(inputStream, HubPlugin::class.java)
      val beanValidationResult = validateHubPluginBean(descriptor)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }
      val vendorInfo = parseHubVendorInfo(descriptor.author)
      descriptor.apply {
        manifest = IOUtils.toString(inputStream, Charsets.UTF_8)
        vendor = vendorInfo.vendor
        vendorEmail = vendorInfo.vendorEmail
        vendorUrl = vendorInfo.vendorUrl
      }
      return PluginCreationSuccess(descriptor, beanValidationResult)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read plugin descriptor from $streamName", e)
      return PluginCreationFail(UnableToReadDescriptor(streamName, e.localizedMessage))
    }
  }
}