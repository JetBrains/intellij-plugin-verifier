package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginInfo

class MockPluginDetailsCache : PluginDetailsCache {
  override fun getPluginDetailsCacheEntry(pluginInfo: PluginInfo): PluginDetailsCache.Result {
    return PluginDetailsCache.Result.FileNotFound("Mock cache does not provide any files")
  }

  override fun close() {
    // no-op
  }
}