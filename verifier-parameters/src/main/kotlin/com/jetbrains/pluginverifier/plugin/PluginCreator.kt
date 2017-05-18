package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.plugin.PluginManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager

object PluginCreator {

  fun createPlugin(pluginDescriptor: PluginDescriptor): CreatePluginResult = when (pluginDescriptor) {
    is PluginDescriptor.ByFileLock -> createPluginByFileLock(pluginDescriptor.fileLock)
    is PluginDescriptor.ByUpdateInfo -> createPluginByUpdateInfo(pluginDescriptor)
    is PluginDescriptor.ByInstance -> pluginDescriptor.createOk
  }

  private fun downloadPluginByUpdateInfo(updateInfo: UpdateInfo): FileLock? = RepositoryManager.getPluginFile(updateInfo)

  private fun createPluginByUpdateInfo(pluginDescriptor: PluginDescriptor.ByUpdateInfo): CreatePluginResult {
    val pluginFileLock = downloadPluginByUpdateInfo(pluginDescriptor.updateInfo)
        ?: return CreatePluginResult.NotFound("Plugin $pluginDescriptor is not found in the Plugin Repository")
    return createPluginByFileLock(pluginFileLock)
  }

  fun createPluginByFileLock(pluginFileLock: FileLock): CreatePluginResult {
    val pluginCreationResult = try {
      PluginManager.getInstance().createPlugin(pluginFileLock.getFile())
    } catch(e: Throwable) {
      pluginFileLock.release()
      throw e
    }

    if (pluginCreationResult is PluginCreationSuccess) {
      try {
        val pluginResolver = Resolver.createPluginResolver(pluginCreationResult.plugin)
        return CreatePluginResult.OK(pluginFileLock, pluginCreationResult, pluginResolver)
      } catch (e: Throwable) {
        pluginFileLock.release()
        throw e
      }
    } else {
      return CreatePluginResult.BadPlugin(pluginCreationResult as PluginCreationFail)
    }
  }

}
