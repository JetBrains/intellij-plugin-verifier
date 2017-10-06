package com.jetbrains.pluginverifier.dependencies.resolution

import com.google.common.collect.ImmutableSet
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.repository.UpdateSelector
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * @author Sergey Patrikeev
 */
class RepositoryDependencyFinder(private val pluginRepository: PluginRepository,
                                 private val updateSelector: UpdateSelector,
                                 private val pluginDetailsProvider: PluginDetailsProvider) : DependencyFinder {

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

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    if (dependency.isModule) {
      return resolveModuleDependency(dependency.id)
    }
    return selectPlugin(dependency.id)
  }

  private fun resolveModuleDependency(moduleId: String): DependencyFinder.Result {
    if (isDefaultModule(moduleId)) {
      return DependencyFinder.Result.Skip
    }
    return resolveDeclaringPlugin(moduleId)
  }

  private fun resolveDeclaringPlugin(moduleId: String): DependencyFinder.Result {
    val pluginId = pluginRepository.getIdOfPluginDeclaringModule(moduleId)
        ?: return DependencyFinder.Result.NotFound("Module '$moduleId' is not found")
    return selectPlugin(pluginId)
  }

  private fun selectPlugin(pluginId: String): DependencyFinder.Result {
    val selectResult = updateSelector.select(pluginId, pluginRepository)
    return when (selectResult) {
      is UpdateSelector.Result.Plugin -> DependencyFinder.Result.FoundCoordinates(selectResult.updateInfo, pluginDetailsProvider)
      is UpdateSelector.Result.NotFound -> DependencyFinder.Result.NotFound(selectResult.reason)
    }
  }
}