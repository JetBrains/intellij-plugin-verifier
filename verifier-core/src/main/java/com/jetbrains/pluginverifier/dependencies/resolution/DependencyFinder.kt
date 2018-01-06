package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginInfo

interface DependencyFinder {

  fun findPluginDependency(dependency: PluginDependency): Result

  sealed class Result {

    data class PluginAndDetailsProvider(val plugin: IdePlugin, val pluginDetailsProvider: PluginDetailsProvider) : Result()

    data class FoundPluginInfo(val pluginInfo: PluginInfo, val pluginDetailsProvider: PluginDetailsProvider) : Result()

    data class FoundOpenPluginWithoutClasses(val plugin: IdePlugin) : Result()

    data class NotFound(val reason: String) : Result()

    data class DefaultIdeaModule(val moduleId: String) : Result()
  }
}