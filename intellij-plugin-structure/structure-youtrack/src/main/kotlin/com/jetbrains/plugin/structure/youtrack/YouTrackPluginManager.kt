package com.jetbrains.plugin.structure.youtrack

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppManifest
import com.jetbrains.plugin.structure.youtrack.problems.createIncorrectYouTrackPluginFileError
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class YouTrackPluginManager private constructor(private val extractDirectory: Path) : PluginManager<YouTrackPlugin> {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(YouTrackPluginManager::class.java)

    const val DESCRIPTOR_NAME = "manifest.json"

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get())
    ): YouTrackPluginManager {
      extractDirectory.createDir()
      return YouTrackPluginManager(extractDirectory)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<YouTrackPlugin> {
    require(pluginFile.exists()) { "Plugin file $pluginFile does not exist" }
    return when {
      pluginFile.isDirectory -> loadPluginInfoFromDirectory(pluginFile)
      pluginFile.isZip() -> loadDescriptorFromZip(pluginFile)
      else -> PluginCreationFail(createIncorrectYouTrackPluginFileError(pluginFile.simpleName))
    }
  }


  private fun loadDescriptorFromZip(pluginFile: Path): PluginCreationResult<YouTrackPlugin> {
    val sizeLimit = Settings.YOUTRACK_PLUGIN_SIZE_LIMIT.getAsLong()
    if (Files.size(pluginFile) > sizeLimit) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
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

  private fun loadPluginInfoFromDirectory(pluginDirectory: Path): PluginCreationResult<YouTrackPlugin> {
    val manifestFile = pluginDirectory.resolve(DESCRIPTOR_NAME)
    if (!manifestFile.exists()) {
      return PluginCreationFail(PluginDescriptorIsNotFound(DESCRIPTOR_NAME))
    }
    val manifestContent = manifestFile.readText()
    val manifest = jacksonObjectMapper().readValue(manifestContent, YouTrackAppManifest::class.java)
    val icons = getIcons(pluginDirectory, manifest)

    return createPlugin(manifest, icons)
  }

  private fun createPlugin(
    descriptor: YouTrackAppManifest,
    icons: List<PluginIcon>
  ): PluginCreationResult<YouTrackPlugin> {
    try {
      val beanValidationResult = validateYouTrackManifest(descriptor)
      if (beanValidationResult.any { it.level == PluginProblem.Level.ERROR }) {
        return PluginCreationFail(beanValidationResult)
      }

      val plugin = with(descriptor) {
        YouTrackPlugin(
          pluginId = name,
          pluginName = title,
          pluginVersion = version,
          description = description,
          url = url,
          changeNotes = changeNotes,
          vendor = vendor?.name,
          vendorUrl = vendor?.url,
          vendorEmail = vendor?.email,
          icons = icons
        )
      }
      return PluginCreationSuccess(plugin, beanValidationResult)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.info("Unable to read app descriptor $DESCRIPTOR_NAME", e)
      return PluginCreationFail(UnableToReadDescriptor(DESCRIPTOR_NAME, e.localizedMessage))
    }
  }

  private fun getIcons(pluginDirectory: Path, manifest: YouTrackAppManifest): List<PluginIcon> {
    fun getIcon(fileName: String?, iconTheme: IconTheme): PluginIcon? {
      if (fileName == null) return null
      val file = pluginDirectory.listFiles().find { it.simpleName == fileName } ?: return null
      return PluginIcon(iconTheme, file.readBytes(), fileName)
    }
    val icons = mutableListOf<PluginIcon>()
    getIcon(manifest.icon, IconTheme.DEFAULT)?.let { icons.add(it) }
    getIcon(manifest.iconDark, IconTheme.DARCULA)?.let { icons.add(it) }
    return icons
  }
}