package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.repository.DownloadPluginResult
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class PluginCreatorImpl(private val pluginRepository: PluginRepository,
                        private val extractDirectory: File) : PluginCreator {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(PluginCreator::class.java)
  }

  override fun createPlugin(pluginCoordinate: PluginCoordinate): CreatePluginResult = when (pluginCoordinate) {
    is PluginCoordinate.ByFile -> createPluginByFile(pluginCoordinate.pluginFile)
    is PluginCoordinate.ByUpdateInfo -> createPluginByUpdateInfo(pluginCoordinate.updateInfo)
  }

  private fun createPluginByUpdateInfo(updateInfo: UpdateInfo): CreatePluginResult {
    val downloadPluginResult = pluginRepository.downloadPluginFile(updateInfo)
    return when (downloadPluginResult) {
      is DownloadPluginResult.Found -> createPluginByFileLock(downloadPluginResult.fileLock)
      is DownloadPluginResult.NotFound -> CreatePluginResult.NotFound(downloadPluginResult.reason)
      is DownloadPluginResult.FailedToDownload -> CreatePluginResult.FailedToDownload(downloadPluginResult.reason)
    }
  }

  private data class IdleFileLock(private val backedFile: File) : FileLock() {
    override fun release() = Unit

    override fun getFile(): File = backedFile
  }

  override fun createPluginByFile(pluginFile: File) = createPluginByFileLock(IdleFileLock(pluginFile))

  override fun createPluginByFileLock(pluginFileLock: FileLock): CreatePluginResult {
    pluginFileLock.closeOnException {
      val pluginFile = pluginFileLock.getFile()

      val creationResult = IdePluginManager.createManager(extractDirectory).createPlugin(pluginFile)
      if (creationResult is PluginCreationSuccess) {
        val pluginClassesLocations = findPluginClassesOrCloseLock(creationResult.plugin, pluginFileLock) ?: return CreatePluginResult.BadPlugin(listOf(UnableToReadPluginClassFilesProblem))
        return CreatePluginResult.OK(creationResult.plugin, creationResult.warnings, pluginClassesLocations, pluginFileLock)
      } else {
        pluginFileLock.close()
        return CreatePluginResult.BadPlugin((creationResult as PluginCreationFail).errorsAndWarnings)
      }
    }
  }

  override fun createResultByExistingPlugin(plugin: IdePlugin): CreatePluginResult {
    val resolver = findPluginClassesOrCloseLock(plugin, null) ?: return CreatePluginResult.BadPlugin(listOf(UnableToReadPluginClassFilesProblem))
    return CreatePluginResult.OK(plugin, emptyList(), resolver, IdleFileLock(File("")))
  }

  private fun findPluginClassesOrCloseLock(plugin: IdePlugin, fileLock: FileLock?): IdePluginClassesLocations? = try {
    IdePluginClassesFinder.findPluginClasses(plugin, additionalKeys = listOf(CompileServerExtensionKey))
  } catch (e: Exception) {
    LOG.info("Unable to read plugin $plugin class files", e)
    fileLock?.closeLogged()
    null
  }

}
