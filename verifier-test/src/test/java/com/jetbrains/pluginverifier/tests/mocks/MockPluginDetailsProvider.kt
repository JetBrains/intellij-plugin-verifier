package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider

/**
 * Created by Sergey.Patrikeev
 */
class MockPluginDetailsProvider(private val coordinatesToDetails: Map<PluginCoordinate, PluginDetails>) : PluginDetailsProvider {
  override fun providePluginDetails(pluginCoordinate: PluginCoordinate): PluginDetails =
      coordinatesToDetails[pluginCoordinate] ?: PluginDetails.NotFound("Not found $pluginCoordinate")

  override fun provideDetailsByExistingPlugins(plugin: IdePlugin): PluginDetails =
      PluginDetails.FoundOpenPluginWithoutClasses(plugin)
}