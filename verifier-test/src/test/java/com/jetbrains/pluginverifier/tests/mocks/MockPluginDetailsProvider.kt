package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Created by Sergey.Patrikeev
 */
class MockPluginDetailsProvider(private val infoToDetails: Map<PluginInfo, PluginDetails>) : PluginDetailsProvider {
  override fun providePluginDetails(pluginInfo: PluginInfo): PluginDetails =
      infoToDetails[pluginInfo] ?: PluginDetails.NotFound("Not found $pluginInfo")

  override fun provideDetailsByExistingPlugins(plugin: IdePlugin): PluginDetails =
      PluginDetails.FoundOpenPluginWithoutClasses(plugin)
}