/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import java.io.Closeable

/**
 * This cache is intended to open and cache [PluginDetails] for
 * use by multiple threads. It is necessary because the details creation may be expensive
 * as it requires downloading the plugin, reading its class files and registering a file lock.
 *
 * The cache must be [closed] [close] on the application shutdown to free all the details.
 */
class PluginDetailsCache(
  cacheSize: Int,
  val pluginFileProvider: PluginFileProvider,
  val pluginDetailsProvider: PluginDetailsProvider
) : Closeable {

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
  fun getPluginDetailsCacheEntry(pluginInfo: PluginInfo): Result {
    return when (val internalResult = internalCache.getResourceCacheEntry(pluginInfo)) {
      is ResourceCacheEntryResult.Found -> {
        val internalEntry = internalResult.resourceCacheEntry
        val pluginDetailsProviderResult = internalEntry.resource
        @Suppress("UNCHECKED_CAST")
        when (pluginDetailsProviderResult) {
          is PluginDetailsProvider.Result.Provided ->
            Result.Provided(internalEntry as ResourceCacheEntry<PluginDetailsProvider.Result.Provided, SizeWeight>)

          is PluginDetailsProvider.Result.InvalidPlugin ->
            Result.InvalidPlugin(internalEntry as ResourceCacheEntry<PluginDetailsProvider.Result.InvalidPlugin, SizeWeight>)

          is PluginDetailsProvider.Result.Failed ->
            Result.Failed(pluginDetailsProviderResult.reason, pluginDetailsProviderResult.error)
        }
      }
      is ResourceCacheEntryResult.Failed -> Result.Failed(internalResult.message, internalResult.error)
      is ResourceCacheEntryResult.NotFound -> Result.FileNotFound(internalResult.message)
    }
  }

  override fun close() = internalCache.close()

  /**
   * Represents possible results of the [getPluginDetailsCacheEntry].
   * It **must be** closed after usage.
   */
  sealed class Result : Closeable {

    /**
     * The [pluginDetails] are successfully provided.
     */
    data class Provided(private val internalEntry: ResourceCacheEntry<PluginDetailsProvider.Result.Provided, SizeWeight>) : Result() {
      /**
       * The provided [PluginDetails].
       *
       * It **must not** be closed directly because it will be closed
       * when this result is closed.
       */
      val pluginDetails: PluginDetails
        get() = internalEntry.resource.pluginDetails

      override fun close() = internalEntry.close()
    }

    /**
     * [PluginDetails] are not provided because the plugin
     * passed to [getPluginDetailsCacheEntry] is invalid.
     */
    data class InvalidPlugin(private val internalEntry: ResourceCacheEntry<PluginDetailsProvider.Result.InvalidPlugin, SizeWeight>) : Result() {

      /**
       * The errors and warnings of the plugin structure.
       */
      val pluginErrors: List<PluginProblem>
        get() = internalEntry.resource.pluginErrors

      val pluginInfo: PluginInfo
        get() = internalEntry.resource.pluginInfo

      override fun close() = internalEntry.close()
    }

    /**
     * [getPluginDetailsCacheEntry] has failed with [error] and presentable reason of [reason].
     */
    data class Failed(val reason: String, val error: Throwable) : Result() {
      override fun close() = Unit
    }

    /**
     * [PluginDetails] are not provided because the file
     * of the plugin passed to [getPluginDetailsCacheEntry] is not found.
     */
    data class FileNotFound(val reason: String) : Result() {
      override fun close() = Unit
    }
  }

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