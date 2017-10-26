package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

//todo: rename the methods
interface PluginDetailsProvider {

  fun fetchPluginDetails(pluginCoordinate: PluginCoordinate): PluginDetails

  fun fetchByExistingPlugins(plugin: IdePlugin): PluginDetails

}