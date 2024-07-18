/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.dependency.DependencyPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

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
    is DependencyPluginInfo -> provideDependencyDetails(key)
    else -> provideFileAndDetails(key)
  }

  private fun provideFileAndDetails(pluginInfo: PluginInfo): ProvideResult<PluginDetailsProvider.Result> {
    return pluginFileProvider
      .getPluginFile(pluginInfo)
      .provideDetails(pluginInfo)
  }

  private fun PluginFileProvider.Result.provideDetails(pluginInfo: PluginInfo): ProvideResult<PluginDetailsProvider.Result> {
    return when (this) {
      is PluginFileProvider.Result.Found -> {
        pluginDetailsProvider
          .providePluginDetails(pluginInfo, pluginFileLock)
          .provided
      }

      is PluginFileProvider.Result.NotFound -> ProvideResult.NotFound(reason)
      is PluginFileProvider.Result.Failed -> ProvideResult.Failed(reason, error)
    }
  }

  private fun provideDependencyDetails(dependency: DependencyPluginInfo): ProvideResult<PluginDetailsProvider.Result> {
    val unwrappedPlugin = dependency.idePlugin
    val unwrappedPluginInfo = dependency.pluginInfo
    return if (unwrappedPlugin != null) {
      pluginDetailsProvider.providePluginDetails(unwrappedPluginInfo, unwrappedPlugin).provided
    } else {
      pluginFileProvider
        .getPluginFile(unwrappedPluginInfo)
        .provideDetails(dependency)
    }
  }

  private val DependencyPluginInfo.idePlugin: IdePlugin?
    get() {
      return when (pluginInfo) {
        is BundledPluginInfo -> pluginInfo.idePlugin
        is LocalPluginInfo -> pluginInfo.idePlugin
        is CustomPluginInfo -> null
        is UpdateInfo -> null
        else -> null
      }
    }

  private val PluginDetailsProvider.Result.provided
    get() = ProvideResult.Provided(this)


}
