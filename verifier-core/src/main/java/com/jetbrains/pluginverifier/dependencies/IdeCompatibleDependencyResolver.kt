package com.jetbrains.pluginverifier.dependencies

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator

class IdeCompatibleDependencyResolver(val ide: Ide) : DependencyResolver {

  private val downloadCompatibleDependencyResolver = DownloadCompatibleDependencyResolver(ide.version)

  override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
    val id = dependency.id
    val existingPlugin = if (dependency.isModule) {
      ide.getPluginByModule(id)
    } else {
      ide.getPluginById(id)
    }
    if (existingPlugin != null) {
      return createDependencyResultByExistingPlugin(existingPlugin)
    }
    return downloadCompatibleDependencyResolver.resolve(dependency)
  }

  private fun createDependencyResultByExistingPlugin(plugin: Plugin): DependencyResolver.Result {
    val pluginCreateResult = PluginCreator.createResultByExistingPlugin(plugin)
    return when (pluginCreateResult) {
      is CreatePluginResult.OK -> DependencyResolver.Result.CreatedResolver(pluginCreateResult.plugin, pluginCreateResult.resolver)
      is CreatePluginResult.BadPlugin -> DependencyResolver.Result.ProblematicDependency(pluginCreateResult.pluginErrorsAndWarnings)
      is CreatePluginResult.NotFound -> DependencyResolver.Result.NotFound(pluginCreateResult.reason)
    }
  }

}