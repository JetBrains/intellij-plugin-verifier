package com.jetbrains.pluginverifier.dependencies

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.intellij.structure.ide.Ide
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo

class DefaultDependencyResolver(val ide: Ide) : DependencyResolver {

  companion object {
    /**
     * The list of IntelliJ plugins which define some modules
     * (e.g. the plugin "org.jetbrains.plugins.ruby" defines a module "com.intellij.modules.ruby")
     */
    private val INTELLIJ_MODULE_TO_CONTAINING_PLUGIN = ImmutableMap.of(
        "com.intellij.modules.ruby", "org.jetbrains.plugins.ruby",
        "com.intellij.modules.php", "com.jetbrains.php",
        "com.intellij.modules.python", "Pythonid",
        "com.intellij.modules.swift.lang", "com.intellij.clion-swift")

    private val IDEA_ULTIMATE_MODULES = ImmutableList.of(
        "com.intellij.modules.platform",
        "com.intellij.modules.lang",
        "com.intellij.modules.vcs",
        "com.intellij.modules.xml",
        "com.intellij.modules.xdebugger",
        "com.intellij.modules.java",
        "com.intellij.modules.ultimate",
        "com.intellij.modules.all")

    private fun isDefaultModule(moduleId: String): Boolean = moduleId in IDEA_ULTIMATE_MODULES

  }

  override fun resolve(dependency: PluginDependency, isModule: Boolean): DependencyResolver.Result {
    if (isModule) {
      return resolveModule(dependency.id)
    } else {
      return resolvePlugin(dependency.id)
    }
  }

  private fun createDependencyResultByExistingPlugin(plugin: Plugin): DependencyResolver.Result {
    val pluginCreateResult = PluginCreator.createResultByExistingPlugin(plugin)
    return when (pluginCreateResult) {
      is CreatePluginResult.OK -> DependencyResolver.Result.CreatedResolver(pluginCreateResult.plugin, pluginCreateResult.resolver)
      is CreatePluginResult.BadPlugin -> DependencyResolver.Result.ProblematicDependency(pluginCreateResult.pluginErrorsAndWarnings)
      is CreatePluginResult.NotFound -> DependencyResolver.Result.NotFound(pluginCreateResult.reason)
    }
  }

  private fun resolvePlugin(dependencyId: String): DependencyResolver.Result {
    val byId = ide.getPluginById(dependencyId)
    if (byId != null) {
      return createDependencyResultByExistingPlugin(byId)
    }
    return downloadLastCompatibleUpdate(dependencyId, ide.version)
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

  private fun resolveModule(dependencyId: String): DependencyResolver.Result {
    if (isDefaultModule(dependencyId)) {
      return DependencyResolver.Result.Skip
    }
    val byModule = ide.getPluginByModule(dependencyId)
    if (byModule != null) {
      return createDependencyResultByExistingPlugin(byModule)
    }

    if (dependencyId in INTELLIJ_MODULE_TO_CONTAINING_PLUGIN) {
      val knownPluginId = INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[dependencyId]!!
      return resolvePlugin(knownPluginId)
    }

    return DependencyResolver.Result.NotFound("Module $dependencyId is not found in ${ide.version}")
  }

}