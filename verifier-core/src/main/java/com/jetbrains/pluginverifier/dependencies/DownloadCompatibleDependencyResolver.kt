package com.jetbrains.pluginverifier.dependencies

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * @author Sergey Patrikeev
 */
class DownloadCompatibleDependencyResolver(val ideVersion: IdeVersion) : DependencyResolver {
  override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
    if (dependency.isModule) {
      throw IllegalArgumentException("This dependency resolver cannot resolve dependencies on module ${dependency.id}")
    }
    return downloadLastCompatibleUpdate(dependency.id, ideVersion)
  }

  private fun downloadLastCompatibleUpdate(pluginId: String, ideVersion: IdeVersion): DependencyResolver.Result {
    val lastUpdate: UpdateInfo = RepositoryManager.getLastCompatibleUpdateOfPlugin(ideVersion, pluginId)
        ?: return DependencyResolver.Result.NotFound("Plugin $pluginId doesn't have a build compatible with $ideVersion")
    return downloadAndOpenPlugin(lastUpdate)
  }

  private fun downloadAndOpenPlugin(updateInfo: UpdateInfo): DependencyResolver.Result {
    val pluginLock: FileLock = RepositoryManager.getPluginFile(updateInfo)
        ?: return DependencyResolver.Result.NotFound("Plugin $updateInfo is not found in the Plugin Repository")
    return getDependencyResultByDownloadedUpdate(pluginLock, updateInfo)
  }

  private fun getDependencyResultByDownloadedUpdate(pluginLock: FileLock, updateInfo: UpdateInfo): DependencyResolver.Result {
    val dependencyCreationResult = PluginCreator.createPluginByFileLock(pluginLock)
    return when (dependencyCreationResult) {
      is CreatePluginResult.OK -> {
        DependencyResolver.Result.Downloaded(dependencyCreationResult.plugin, dependencyCreationResult.resolver, updateInfo, pluginLock)
      }
      is CreatePluginResult.BadPlugin -> {
        pluginLock.release()
        DependencyResolver.Result.ProblematicDependency(dependencyCreationResult.pluginErrorsAndWarnings)
      }
      is CreatePluginResult.NotFound -> {
        pluginLock.release()
        DependencyResolver.Result.NotFound(dependencyCreationResult.reason)
      }
    }
  }


}