package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable

class PluginDetailsCache(cacheSize: Int,
                         pluginRepository: PluginRepository,
                         pluginDetailsProvider: PluginDetailsProvider) : Closeable {

  private val resourceCache = ResourceCache(
      cacheSize.toLong(),
      PluginDetailsResourceProvider(pluginRepository, pluginDetailsProvider),
      { it.close() },
      "PluginDetailsCache"
  )

  fun getPluginDetails(updateInfo: UpdateInfo): ResourceCacheEntry<PluginDetails> =
      (resourceCache.getResourceCacheEntry(updateInfo) as ResourceCacheEntryResult.Found).resourceCacheEntry

  private class PluginDetailsResourceProvider(val pluginRepository: PluginRepository,
                                              val pluginDetailsProvider: PluginDetailsProvider) : ResourceProvider<UpdateInfo, PluginDetails> {

    override fun provide(key: UpdateInfo): ProvideResult<PluginDetails> {
      val pluginCoordinate = PluginCoordinate.ByUpdateInfo(key, pluginRepository)
      val pluginDetails = pluginDetailsProvider.providePluginDetails(pluginCoordinate)
      return ProvideResult.Provided(pluginDetails)
    }
  }

  override fun close() = resourceCache.close()

}