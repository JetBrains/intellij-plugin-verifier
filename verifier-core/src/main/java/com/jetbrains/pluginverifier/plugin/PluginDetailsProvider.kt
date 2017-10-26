package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

interface PluginDetailsProvider {

  fun providePluginDetails(pluginCoordinate: PluginCoordinate): PluginDetails

  fun provideDetailsByExistingPlugins(plugin: IdePlugin): PluginDetails

}