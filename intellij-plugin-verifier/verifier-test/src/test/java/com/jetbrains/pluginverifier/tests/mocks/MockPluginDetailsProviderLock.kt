/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks

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
  }
}
