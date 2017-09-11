package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.misc.closeLogged
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

  fun createPluginByUpdateInfo(updateInfo: UpdateInfo): CreatePluginResult {
    val pluginFileLock = RepositoryManager.getPluginFile(updateInfo)
        ?: return CreatePluginResult.NotFound("Plugin $updateInfo is not found in the Plugin Repository")
    return createPluginByFileLock(pluginFileLock)
  }

  private data class IdleFileLock(private val backedFile: File) : FileLock() {
    override fun release() = Unit

    override fun getFile(): File = backedFile
  }

  fun createPluginByFile(pluginFile: File) = createPluginByFileLock(IdleFileLock(pluginFile))

  fun createPluginByFileLock(pluginFileLock: FileLock): CreatePluginResult {
    pluginFileLock.closeOnException {
      val pluginFile = pluginFileLock.getFile()

      val creationResult = IdePluginManager.getInstance().createPlugin(pluginFile)
      if (creationResult is PluginCreationSuccess) {
        val pluginResolver = createResolverByPluginOrCloseLock(creationResult.plugin, pluginFileLock) ?: return CreatePluginResult.BadPlugin(listOf(UnableToReadPluginClassFilesProblem))
        return CreatePluginResult.OK(creationResult.plugin, creationResult.warnings, pluginResolver, pluginFileLock)
      } else {
        pluginFileLock.close()
        return CreatePluginResult.BadPlugin((creationResult as PluginCreationFail).errorsAndWarnings)
      }
    }
  }

  fun createResultByExistingPlugin(plugin: IdePlugin): CreatePluginResult {
    val resolver = createResolverByPluginOrCloseLock(plugin, null) ?: return CreatePluginResult.BadPlugin(listOf(UnableToReadPluginClassFilesProblem))
    return CreatePluginResult.OK(plugin, emptyList(), resolver, IdleFileLock(File("")))
  }

  private fun createResolverByPluginOrCloseLock(plugin: IdePlugin, fileLock: FileLock?): Resolver? = try {
    Resolver.createPluginResolver(plugin)
  } catch (e: Exception) {
    LOG.debug("Unable to read plugin $plugin class files", e)
    fileLock?.closeLogged()
    null
  }

  object UnableToReadPluginClassFilesProblem : PluginProblem() {
    override val level: PluginProblem.Level = PluginProblem.Level.ERROR
    override val message: String = "Unable to read plugin class files"
  }
}
