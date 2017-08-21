package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.plugin.Plugin
import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.plugin.PluginManager
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object PluginCreator {

  private val LOG: Logger = LoggerFactory.getLogger(PluginCreator::class.java)

  fun createPlugin(pluginCoordinate: PluginCoordinate): CreatePluginResult = when (pluginCoordinate) {
    is PluginCoordinate.ByFile -> createPluginByFile(pluginCoordinate.pluginFile)
    is PluginCoordinate.ByUpdateInfo -> createPluginByUpdateInfo(pluginCoordinate.updateInfo)
  }

  private fun downloadPluginByUpdateInfo(updateInfo: UpdateInfo): FileLock? = RepositoryManager.getPluginFile(updateInfo)

  fun createPluginByUpdateInfo(updateInfo: UpdateInfo): CreatePluginResult {
    val pluginFileLock = downloadPluginByUpdateInfo(updateInfo)
        ?: return CreatePluginResult.NotFound("Plugin $updateInfo is not found in the Plugin Repository")
    return createPluginByFileLock(pluginFileLock)
  }

  private data class IdleFileLock(private val backedFile: File) : FileLock() {
    override fun release() = Unit

    override fun getFile(): File = backedFile
  }

  fun createPluginByFile(pluginFile: File) = createPluginResultByFileAndRegisterLock(pluginFile, IdleFileLock(pluginFile))

  fun createPluginByFileLock(pluginFileLock: FileLock): CreatePluginResult =
      createPluginResultByFileAndRegisterLock(pluginFileLock.getFile(), pluginFileLock)

  private fun createPluginResultByFileAndRegisterLock(pluginFile: File, pluginFileLock: FileLock): CreatePluginResult {
    pluginFileLock.closeOnException {
      val pluginCreationResult = PluginManager.getInstance().createPlugin(pluginFile)
      if (pluginCreationResult is PluginCreationSuccess) {
        val pluginResolver = Resolver.createPluginResolver(pluginCreationResult.plugin)
        return CreatePluginResult.OK(pluginCreationResult.plugin, pluginCreationResult.warnings, pluginResolver, pluginFileLock)
      } else {
        pluginFileLock.close()
        return CreatePluginResult.BadPlugin((pluginCreationResult as PluginCreationFail).errorsAndWarnings)
      }
    }
  }

  fun createResultByExistingPlugin(plugin: Plugin): CreatePluginResult {
    val resolver = try {
      Resolver.createPluginResolver(plugin)
    } catch (e: Exception) {
      LOG.debug("Unable to read plugin $plugin class files", e)
      return CreatePluginResult.BadPlugin(listOf(UnableToReadPluginClassFilesProblem))
    }
    return CreatePluginResult.OK(plugin, emptyList(), resolver, IdleFileLock(File("")))
  }

  object UnableToReadPluginClassFilesProblem : PluginProblem() {
    override val level: PluginProblem.Level = PluginProblem.Level.ERROR
    override val message: String = "Unable to read plugin class files"
  }
}
