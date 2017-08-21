package com.jetbrains.pluginverifier.dependencies

import com.google.common.collect.ImmutableSet
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

  private companion object {
    val IDEA_ULTIMATE_MODULES: Set<String> = ImmutableSet.of(
        "com.intellij.modules.platform",
        "com.intellij.modules.lang",
        "com.intellij.modules.vcs",
        "com.intellij.modules.xml",
        "com.intellij.modules.xdebugger",
        "com.intellij.modules.java",
        "com.intellij.modules.ultimate",
        "com.intellij.modules.all")

    fun isDefaultModule(moduleId: String): Boolean = moduleId in IDEA_ULTIMATE_MODULES
  }

  override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
    if (dependency.isModule) {
      return resolveModuleDependency(dependency.id)
    }
    return downloadLastCompatibleUpdate(dependency.id)
  }

  private fun resolveModuleDependency(moduleId: String): DependencyResolver.Result {
    if (isDefaultModule(moduleId)) {
      return DependencyResolver.Result.Skip
    }
    return resolveDeclaringPlugin(moduleId)
  }

  private fun resolveDeclaringPlugin(moduleId: String): DependencyResolver.Result {
    val pluginId = RepositoryManager.getIdOfPluginDeclaringModule(moduleId)
        ?: return DependencyResolver.Result.NotFound("Module '$moduleId' is not found in $ideVersion")
    return downloadLastCompatibleUpdate(pluginId)
  }

  private fun downloadLastCompatibleUpdate(pluginId: String): DependencyResolver.Result {
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