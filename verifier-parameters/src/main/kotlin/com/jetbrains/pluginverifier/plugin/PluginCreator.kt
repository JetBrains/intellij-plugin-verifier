package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.impl.domain.PluginCreationFailImpl
import com.intellij.structure.impl.domain.PluginCreationSuccessImpl
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.plugin.PluginManager
import com.intellij.structure.problems.UnableToReadPluginClassFiles
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.CloseIgnoringResolver
import java.io.File

object PluginCreator {

  fun createPlugin(pluginDescriptor: PluginDescriptor): CreatePluginResult = when (pluginDescriptor) {
    is PluginDescriptor.ByFileLock -> createPluginByFile(pluginDescriptor.fileLock.getFile())
    is PluginDescriptor.ByUpdateInfo -> createPluginByUpdateInfo(pluginDescriptor.updateInfo)
    is PluginDescriptor.ByInstance -> getNonCloseableOkResult(pluginDescriptor.createOk)
  }

  fun getNonCloseableOkResult(createOk: CreatePluginResult.OK): CreatePluginResult.OK {
    val copyResolver = CloseIgnoringResolver(createOk.resolver)
    return CreatePluginResult.OK(createOk.success, copyResolver)
  }

  private fun downloadPluginByUpdateInfo(updateInfo: UpdateInfo): FileLock? = RepositoryManager.getPluginFile(updateInfo)

  private fun createPluginByUpdateInfo(updateInfo: UpdateInfo): CreatePluginResult {
    val pluginFileLock = downloadPluginByUpdateInfo(updateInfo)
        ?: return CreatePluginResult.NotFound("Plugin $updateInfo is not found in the Plugin Repository")
    return createPluginByFile(pluginFileLock.getFile())
  }

  fun createPluginByFile(pluginFile: File): CreatePluginResult {
    val pluginCreationResult = PluginManager.getInstance().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationSuccess) {
      val pluginResolver = Resolver.createPluginResolver(pluginCreationResult.plugin)
      return CreatePluginResult.OK(pluginCreationResult, pluginResolver)
    } else {
      return CreatePluginResult.BadPlugin(pluginCreationResult as PluginCreationFail)
    }
  }

  fun createResolverForExistingPlugin(plugin: Plugin): CreatePluginResult {
    val resolver = try {
      Resolver.createPluginResolver(plugin)
    } catch (e: Exception) {
      return CreatePluginResult.BadPlugin(PluginCreationFailImpl(listOf(UnableToReadPluginClassFiles(plugin.originalFile))))
    }
    return CreatePluginResult.OK(PluginCreationSuccessImpl(plugin, emptyList()), resolver)
  }
}
