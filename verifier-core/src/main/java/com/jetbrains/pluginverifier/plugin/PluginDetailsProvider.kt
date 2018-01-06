package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo

interface PluginDetailsProvider {

  fun providePluginDetails(pluginInfo: PluginInfo): PluginDetails

  fun provideDetailsByExistingPlugins(plugin: IdePlugin): PluginDetails

}