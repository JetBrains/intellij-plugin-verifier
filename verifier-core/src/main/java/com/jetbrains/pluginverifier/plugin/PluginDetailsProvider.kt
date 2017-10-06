package com.jetbrains.pluginverifier.plugin

interface PluginDetailsProvider {

  fun fetchPluginDetails(pluginCoordinate: PluginCoordinate): PluginDetails

}