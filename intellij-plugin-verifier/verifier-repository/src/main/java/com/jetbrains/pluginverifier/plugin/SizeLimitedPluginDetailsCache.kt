/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo

/**
 * This cache is intended to open and cache [PluginDetails] for
 * use by multiple threads. It is necessary because the details creation may be expensive
 * as it requires downloading the plugin, reading its class files and registering a file lock.
 *
 * The cache must be [closed] [close] on the application shutdown to free all the details.
 */
class SizeLimitedPluginDetailsCache(
  cacheSize: Int,
  pluginFileProvider: PluginFileProvider,
  pluginDetailsProvider: PluginDetailsProvider
) : PluginDetailsCache {

  private val internalCache = createSizeLimitedResourceCache(
    cacheSize,
    PluginDetailsResourceProvider(pluginFileProvider, pluginDetailsProvider),
    { it.close() },
    "PluginDetailsCache"
  )

  /**
   * Provides the [PluginDetails] of the given [pluginInfo] wrapped in a [Result].
   */
  @Throws(InterruptedException::class)
  override fun getPluginDetailsCacheEntry(pluginInfo: PluginInfo): PluginDetailsCache.Result {
    return when (val internalResult = internalCache.getResourceCacheEntry(pluginInfo)) {
      is ResourceCacheEntryResult.Found -> {
        val internalEntry = internalResult.resourceCacheEntry
        val pluginDetailsProviderResult = internalEntry.resource
        @Suppress("UNCHECKED_CAST")
        when (pluginDetailsProviderResult) {
          is PluginDetailsProvider.Result.Provided ->
            PluginDetailsCache.Result.Provided(internalEntry as ResourceCacheEntry<PluginDetailsProvider.Result.Provided, SizeWeight>)

          is PluginDetailsProvider.Result.InvalidPlugin ->
            PluginDetailsCache.Result.InvalidPlugin(internalEntry as ResourceCacheEntry<PluginDetailsProvider.Result.InvalidPlugin, SizeWeight>)

          is PluginDetailsProvider.Result.Failed ->
            PluginDetailsCache.Result.Failed(pluginDetailsProviderResult.reason, pluginDetailsProviderResult.error)
        }
      }
      is ResourceCacheEntryResult.Failed -> PluginDetailsCache.Result.Failed(internalResult.message, internalResult.error)
      is ResourceCacheEntryResult.NotFound -> PluginDetailsCache.Result.FileNotFound(internalResult.message)
    }
  }

  override fun close() = internalCache.close()

}

/**
 * Bridge utility class that maps [PluginDetailsProvider] to [ResourceProvider].
 */
private class PluginDetailsResourceProvider(
  val pluginFileProvider: PluginFileProvider,
  val pluginDetailsProvider: PluginDetailsProvider
) : ResourceProvider<PluginInfo, PluginDetailsProvider.Result> {

  override fun provide(key: PluginInfo) = when (key) {
    is LocalPluginInfo -> ProvideResult.Provided(pluginDetailsProvider.providePluginDetails(key, key.idePlugin))
    is BundledPluginInfo -> ProvideResult.Provided(pluginDetailsProvider.providePluginDetails(key, key.idePlugin))
    else -> provideFileAndDetails(key)
  }

  private fun provideFileAndDetails(pluginInfo: PluginInfo): ProvideResult<PluginDetailsProvider.Result> {
    return with(pluginFileProvider.getPluginFile(pluginInfo)) {
      when (this) {
        is PluginFileProvider.Result.Found -> {
          val pluginDetailsResult = pluginDetailsProvider.providePluginDetails(pluginInfo, pluginFileLock)
          ProvideResult.Provided(pluginDetailsResult)
        }
        is PluginFileProvider.Result.NotFound -> ProvideResult.NotFound(reason)
        is PluginFileProvider.Result.Failed -> ProvideResult.Failed(reason, error)
      }
    }
  }
}