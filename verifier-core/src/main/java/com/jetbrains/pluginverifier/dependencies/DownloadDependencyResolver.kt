package com.jetbrains.pluginverifier.dependencies

import com.google.common.collect.ImmutableSet
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.DownloadPluginResult
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * @author Sergey Patrikeev
 */
class DownloadDependencyResolver(private val dependencySelector: DependencySelector,
                                 private val pluginRepository: PluginRepository,
                                 private val pluginCreator: PluginCreator) : DependencyResolver {

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
    return selectAndDownloadPlugin(dependency.id)
  }

  private fun resolveModuleDependency(moduleId: String): DependencyResolver.Result {
    if (isDefaultModule(moduleId)) {
      return DependencyResolver.Result.Skip
    }
    return resolveDeclaringPlugin(moduleId)
  }

  private fun resolveDeclaringPlugin(moduleId: String): DependencyResolver.Result {
    val pluginId = pluginRepository.getIdOfPluginDeclaringModule(moduleId) ?: return DependencyResolver.Result.NotFound("Module '$moduleId' is not found")
    return selectAndDownloadPlugin(pluginId)
  }

  private fun selectAndDownloadPlugin(pluginId: String): DependencyResolver.Result {
    val selectResult = dependencySelector.select(pluginId, pluginRepository)
    return when (selectResult) {
      is DependencySelector.Result.Plugin -> downloadAndOpenPlugin(selectResult.updateInfo)
      is DependencySelector.Result.NotFound -> DependencyResolver.Result.NotFound(selectResult.reason)
    }
  }

  private fun downloadAndOpenPlugin(updateInfo: UpdateInfo): DependencyResolver.Result {
    val downloadPluginResult = pluginRepository.downloadPluginFile(updateInfo)
    return when (downloadPluginResult) {
      is DownloadPluginResult.Found -> getDependencyResultByDownloadedUpdate(downloadPluginResult.fileLock, updateInfo)
      is DownloadPluginResult.NotFound -> DependencyResolver.Result.NotFound(downloadPluginResult.reason)
      is DownloadPluginResult.FailedToDownload -> DependencyResolver.Result.FailedToDownload(downloadPluginResult.reason)
    }
  }

  private fun getDependencyResultByDownloadedUpdate(pluginLock: FileLock, updateInfo: UpdateInfo): DependencyResolver.Result {
    val dependencyCreationResult = pluginCreator.createPluginByFileLock(pluginLock)
    if (dependencyCreationResult !is CreatePluginResult.OK) {
      pluginLock.release()
    }
    return when (dependencyCreationResult) {
      is CreatePluginResult.OK -> DependencyResolver.Result.Downloaded(dependencyCreationResult.plugin, dependencyCreationResult.locationsContainer, updateInfo, pluginLock)
      is CreatePluginResult.BadPlugin -> DependencyResolver.Result.ProblematicDependency(dependencyCreationResult.pluginErrorsAndWarnings)
      is CreatePluginResult.NotFound -> DependencyResolver.Result.NotFound(dependencyCreationResult.reason)
      is CreatePluginResult.FailedToDownload -> DependencyResolver.Result.FailedToDownload(dependencyCreationResult.reason)
    }
  }


}