package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache.Result.Provided
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import java.io.Closeable
import java.time.Duration

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

  private val pluginDetailsCache = createSizeLimitedResourceCache(
      cacheSize,
      PluginDetailsResourceProvider(pluginFileProvider, pluginDetailsProvider),
      { it.close() },
      "PluginDetailsCache"
  )

  /**
   * Provides the [PluginDetails] of the given [pluginInfo]
   * wrapped in a [Result].
   */
  @Throws(InterruptedException::class)
  fun getPluginDetailsCacheEntry(pluginInfo: PluginInfo): Result {
    val cacheEntryResult = pluginDetailsCache.getResourceCacheEntry(pluginInfo)
    return when (cacheEntryResult) {
      is ResourceCacheEntryResult.Found -> {
        val cacheEntry: ResourceCacheEntry<PluginDetailsProvider.Result, SizeWeight> = cacheEntryResult.resourceCacheEntry
        val resource = cacheEntry.resource
        @Suppress("UNCHECKED_CAST")
        when (resource) {
          is PluginDetailsProvider.Result.Provided ->
            Result.Provided(cacheEntry as ResourceCacheEntry<PluginDetailsProvider.Result.Provided, SizeWeight>)

          is PluginDetailsProvider.Result.InvalidPlugin ->
            Result.InvalidPlugin(cacheEntry as ResourceCacheEntry<PluginDetailsProvider.Result.InvalidPlugin, SizeWeight>)

          is PluginDetailsProvider.Result.Failed ->
            Result.Failed(resource.reason, resource.error)
        }
      }
      is ResourceCacheEntryResult.Failed -> Result.Failed(cacheEntryResult.message, cacheEntryResult.error)
      is ResourceCacheEntryResult.NotFound -> Result.FileNotFound(cacheEntryResult.message)
    }
  }

  override fun close() = pluginDetailsCache.close()

  /**
   * Represents possible results of the [getPluginDetailsCacheEntry].
   * It must be closed after usage to release the [Provided.resourceCacheEntry].
   */
  sealed class Result : Closeable {

    /**
     * The [pluginDetails] are successfully provided.
     * They are locked with [resourceCacheEntry].
     */
    data class Provided(private val resourceCacheEntry: ResourceCacheEntry<PluginDetailsProvider.Result.Provided, SizeWeight>) : Result() {
      /**
       * The provided [PluginDetails].
       *
       * It **must not** be closed directly because it will be closed
       * when this result is closed.
       */
      val pluginDetails: PluginDetails
        get() = resourceCacheEntry.resource.pluginDetails

      val fetchDuration: Duration
        get() = resourceCacheEntry.resource.fetchDuration

      val pluginSize: SpaceAmount
        get() = resourceCacheEntry.resource.pluginSize

      override fun close() = resourceCacheEntry.close()
    }

    /**
     * The [PluginDetails] are not provided because the plugin
     * passed to [getPluginDetailsCacheEntry] is [invalid] [pluginErrors].
     */
    data class InvalidPlugin(private val resourceCacheEntry: ResourceCacheEntry<PluginDetailsProvider.Result.InvalidPlugin, SizeWeight>) : Result() {

      /**
       * The [errors] [PluginProblem.Level.ERROR] and [warnings] [PluginProblem.Level.WARNING] of the plugin structure.
       */
      val pluginErrors: List<PluginProblem>
        get() = resourceCacheEntry.resource.pluginErrors

      val pluginInfo: PluginInfo
        get() = resourceCacheEntry.resource.pluginInfo

      val fetchDuration: Duration
        get() = resourceCacheEntry.resource.fetchDuration

      val pluginSize: SpaceAmount
        get() = resourceCacheEntry.resource.pluginSize

      override fun close() = resourceCacheEntry.close()
    }

    /**
     * The [getPluginDetailsCacheEntry] is failed with [error] and presentable reason of [reason].
     */
    data class Failed(val reason: String, val error: Throwable) : Result() {
      override fun close() = Unit
    }

    /**
     * The [PluginDetails] are not provided because the file
     * of the plugin passed to [getPluginDetailsCacheEntry] is not found.
     */
    data class FileNotFound(val reason: String) : Result() {
      override fun close() = Unit
    }
  }

  /**
   * The bridge class that friends the [ResourceProvider] and [PluginDetailsProvider].
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
          is PluginFileProvider.Result.Found -> ProvideResult.Provided(pluginDetailsProvider.providePluginDetails(pluginInfo, pluginFileLock))
          is PluginFileProvider.Result.NotFound -> ProvideResult.NotFound(reason)
          is PluginFileProvider.Result.Failed -> ProvideResult.Failed(reason, error)
        }
      }
    }
  }

}