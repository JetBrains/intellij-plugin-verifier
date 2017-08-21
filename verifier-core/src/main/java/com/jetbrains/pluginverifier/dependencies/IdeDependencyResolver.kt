package com.jetbrains.pluginverifier.dependencies

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.intellij.structure.ide.Ide
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator

class IdeDependencyResolver(val ide: Ide) : DependencyResolver {

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

  private val downloadCompatibleDependencyResolver = DownloadCompatibleDependencyResolver(ide.version)

  override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
    return if (dependency.isModule) {
      resolveModule(dependency)
    } else {
      resolvePlugin(dependency)
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

  private fun resolvePlugin(dependency: PluginDependency): DependencyResolver.Result {
    val byId = ide.getPluginById(dependency.id)
    if (byId != null) {
      return createDependencyResultByExistingPlugin(byId)
    }
    return downloadCompatibleDependencyResolver.resolve(dependency)
  }

  private fun resolveModule(dependency: PluginDependency): DependencyResolver.Result {
    val moduleId = dependency.id
    if (isDefaultModule(moduleId)) {
      return DependencyResolver.Result.Skip
    }
    val byModule = ide.getPluginByModule(moduleId)
    if (byModule != null) {
      return createDependencyResultByExistingPlugin(byModule)
    }

    val containingPluginId = INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]
    if (containingPluginId != null) {
      val containingPluginDependency = PluginDependencyImpl(containingPluginId, dependency.isOptional, false)
      return resolvePlugin(containingPluginDependency)
    }

    return DependencyResolver.Result.NotFound("Module $moduleId is not found in ${ide.version}")
  }

}