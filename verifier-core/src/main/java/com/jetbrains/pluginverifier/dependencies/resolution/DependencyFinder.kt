package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider

interface DependencyFinder {

  fun findPluginDependency(dependency: PluginDependency): Result

  sealed class Result {

    data class PluginAndDetailsProvider(val plugin: IdePlugin, val pluginDetailsProvider: PluginDetailsProvider) : Result()

    data class FoundCoordinates(val pluginCoordinate: PluginCoordinate,
                                val pluginDetailsProvider: PluginDetailsProvider) : Result()

    data class FoundOpenPluginWithoutClasses(val plugin: IdePlugin) : Result()

    data class FoundOpenPluginAndClasses(val plugin: IdePlugin,
                                         val warnings: List<PluginProblem>,
                                         val pluginClassesLocations: IdePluginClassesLocations) : Result()

    data class NotFound(val reason: String) : Result()

    data class DefaultIdeaModule(val moduleId: String) : Result()
  }
}