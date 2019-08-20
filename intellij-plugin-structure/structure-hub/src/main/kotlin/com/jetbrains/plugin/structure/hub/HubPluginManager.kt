package com.jetbrains.plugin.structure.hub

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.hub.problems.createIncorrectHubPluginFile
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

class HubPluginManager private constructor() : PluginManager<HubPlugin> {
  companion object {
    private const val DESCRIPTOR_NAME = "manifest.json"

    private val LOG: Logger = LoggerFactory.getLogger(HubPluginManager::class.java)

    fun createManager(): HubPluginManager =
        HubPluginManager()
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
    ZipFile(pluginFile).use {
      loadDescriptorFromZip(it)
    }
  } catch (e: IOException) {
    LOG.info("Unable to extract plugin zip: $pluginFile", e)
    PluginCreationFail(UnableToExtractZip())
  }

  private fun loadDescriptorFromZip(pluginFile: ZipFile): PluginCreationResult<HubPlugin> {
    val errors = validateHubZipFile(pluginFile)
    if (errors != null) {
      return errors
    }
    val descriptorEntry = pluginFile.getEntry(DESCRIPTOR_NAME)
        ?: return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    val manifest = IOUtils.toString(pluginFile.getInputStream(descriptorEntry), Charsets.UTF_8)
    return loadDescriptorFromStream(pluginFile.name, pluginFile.getInputStream(descriptorEntry), manifest)
  }

  private fun loadDescriptorFromDirectory(pluginDirectory: File): PluginCreationResult<HubPlugin> {
    val descriptorFile = File(pluginDirectory, DESCRIPTOR_NAME)
    if (descriptorFile.exists()) {
      val manifest = IOUtils.toString(descriptorFile.inputStream(), Charsets.UTF_8)
      return descriptorFile.inputStream().use { loadDescriptorFromStream(descriptorFile.path, it, manifest) }
    }
    return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
  }

  private fun loadDescriptorFromStream(streamName: String, inputStream: InputStream, manifest: String): PluginCreationResult<HubPlugin> {
    try {
      val descriptor = jacksonObjectMapper()
          .readValue(inputStream, HubPlugin::class.java)
      val vendorInfo = parseHubVendorInfo(descriptor.author)
      descriptor.apply {
        this.manifest = manifest
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
      LOG.info("Unable to read plugin descriptor from $streamName", e)
      return PluginCreationFail(UnableToReadDescriptor(streamName, e.localizedMessage))
    }
  }
}