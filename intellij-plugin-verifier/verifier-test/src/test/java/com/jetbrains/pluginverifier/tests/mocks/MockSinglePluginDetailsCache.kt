/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry

class MockSinglePluginDetailsCache(
  private val supportedPluginId: String,
  private val pluginDetailsProvider: PluginDetailsProvider
) : PluginDetailsCache {
  override fun getPluginDetailsCacheEntry(pluginInfo: PluginInfo): PluginDetailsCache.Result {
    return if (supportedPluginId != pluginInfo.pluginId) {
      fail(pluginInfo)
    } else {
      MockPluginDetailsProviderLock.of(pluginInfo, pluginDetailsProvider)?.let { lock ->
        PluginDetailsCache.Result.Provided(ResourceCacheEntry(lock))
      } ?: fail(pluginInfo)
    }
  }

  private fun fail(pluginInfo: PluginInfo): PluginDetailsCache.Result.Failed {
    val msg = "Unsupported plugin: ${pluginInfo.pluginId}"
    return PluginDetailsCache.Result.Failed(msg, IllegalStateException(msg))
  }

  override fun close() = Unit
}