/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.WithIdePlugin
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.resources.ResourceInfo
import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.time.Instant

class MockPluginDetailsProviderLock private constructor(
  resourceInfo: ResourceInfo<PluginDetailsProvider.Result.Provided, SizeWeight>
) :
  ResourceLock<PluginDetailsProvider.Result.Provided, SizeWeight>(
    lockTime = Instant.now(),
    resourceInfo = resourceInfo
  ) {

  override fun release() = Unit

  companion object {
    fun of(pluginInfo: PluginInfo, pluginDetailsProvider: PluginDetailsProvider): MockPluginDetailsProviderLock? {
      if (pluginInfo is WithIdePlugin) {
        val details = pluginDetailsProvider.providePluginDetails(pluginInfo, pluginInfo.idePlugin)
        if (details is PluginDetailsProvider.Result.Provided) {
          val resource = ResourceInfo(details, SizeWeight(1))
          return MockPluginDetailsProviderLock(resource)
        }
      }
      return null
    }

    fun of(plugin: IdePlugin): MockPluginDetailsProviderLock? {
      return of(plugin.getDetails())
    }

    fun of(plugin: IdePlugin, pluginClassesLocation: IdePluginClassesLocations): MockPluginDetailsProviderLock? {
      val details = plugin.getDetails(pluginClassesLocation)
      return of(details)
    }

    fun ofBundledPlugin(plugin: IdePlugin, ide: Ide): MockPluginDetailsProviderLock? {
      val info = plugin.bundledPluginInfo(ide.version)
      val details = plugin.getDetails(info)
      return of(details)
    }

    private fun of(details: PluginDetails): MockPluginDetailsProviderLock {
      val providedDetails = PluginDetailsProvider.Result.Provided(details)
      val resource = ResourceInfo(providedDetails, SizeWeight(1))
      return MockPluginDetailsProviderLock(resource)
    }
  }
}
