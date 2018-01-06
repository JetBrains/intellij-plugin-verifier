package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable

class PluginDetailsCache(cacheSize: Int,
                         pluginDetailsProvider: PluginDetailsProvider) : Closeable {

  private val resourceCache = ResourceCache(
      cacheSize.toLong(),
      PluginDetailsResourceProvider(pluginDetailsProvider),
      { it.close() },
      "PluginDetailsCache"
  )

  fun getPluginDetails(updateInfo: UpdateInfo): ResourceCacheEntry<PluginDetails> =
      (resourceCache.getResourceCacheEntry(updateInfo) as ResourceCacheEntryResult.Found).resourceCacheEntry

  private class PluginDetailsResourceProvider(val pluginDetailsProvider: PluginDetailsProvider) : ResourceProvider<UpdateInfo, PluginDetails> {

    override fun provide(key: UpdateInfo): ProvideResult<PluginDetails> {
      val pluginDetails = pluginDetailsProvider.providePluginDetails(key)
      return ProvideResult.Provided(pluginDetails)
    }
  }

  override fun close() = resourceCache.close()

}