package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider

interface DependencyResolver {

  fun findPluginDependency(dependency: PluginDependency): Result

  sealed class Result {

    abstract fun getPluginDetails(): PluginDetails

    data class FoundCoordinates(private val pluginCoordinate: PluginCoordinate,
                                private val pluginDetailsProvider: PluginDetailsProvider) : Result() {

      override fun getPluginDetails() = pluginDetailsProvider.fetchPluginDetails(pluginCoordinate)
    }

    data class FoundOpenPluginWithoutClasses(private val plugin: IdePlugin) : Result() {

      override fun getPluginDetails(): PluginDetails = PluginDetails.FoundOpenPluginWithoutClasses(plugin)

    }

    data class FoundOpenPluginAndClasses(private val plugin: IdePlugin,
                                         private val warnings: List<PluginProblem>,
                                         private val pluginClassesLocations: IdePluginClassesLocations) : Result() {

      override fun getPluginDetails() = PluginDetails.FoundOpenPluginAndClasses(plugin, pluginClassesLocations, warnings)
    }

    data class NotFound(val reason: String) : Result() {
      override fun getPluginDetails() = PluginDetails.NotFound(reason)
    }

    object Skip : Result() {
      override fun getPluginDetails() = PluginDetails.NotFound("Skipped")
    }
  }
}