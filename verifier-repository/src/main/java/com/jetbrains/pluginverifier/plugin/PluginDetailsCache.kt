package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache.Result.Provided
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
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

  private val resourceCache = createSizeLimitedResourceCache(
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
    return with(resourceCache.getResourceCacheEntry(pluginInfo)) {
      when (this) {
        is ResourceCacheEntryResult.Found -> {
          with(resourceCacheEntry.resource) {
            when (this) {
              is PluginDetailsProvider.Result.Provided ->
                Result.Provided(resourceCacheEntry, pluginDetails)

              is PluginDetailsProvider.Result.InvalidPlugin ->
                Result.InvalidPlugin(resourceCacheEntry, pluginErrors)

              is PluginDetailsProvider.Result.Failed ->
                Result.Failed(reason, error)
            }
          }
        }
        is ResourceCacheEntryResult.Failed -> Result.Failed(message, error)
        is ResourceCacheEntryResult.NotFound -> Result.FileNotFound(message)
      }
    }
  }

  override fun close() = resourceCache.close()

  /**
   * Represents possible results of the [getPluginDetailsCacheEntry].
   * It must be closed after usage to release the [Provided.resourceCacheEntry].
   */
  sealed class Result : Closeable {

    /**
     * The [pluginDetails] are successfully provided.
     */
    data class Provided(
        /**
         * [ResourceCacheEntry] that protects the
         * [pluginDetails] until the entry is closed.
         */
        private val resourceCacheEntry: ResourceCacheEntry<PluginDetailsProvider.Result>,

        /**
         * The provided [PluginDetails].
         *
         * It _must not_ be closed directly as it will be closed
         * by the [ResourceCache] at the entry disposition time.
         */
        val pluginDetails: PluginDetails
    ) : Result() {

      override fun close() = resourceCacheEntry.close()
    }

    /**
     * The [PluginDetails] are not provided because the plugin
     * passed to [getPluginDetailsCacheEntry] is [invalid] [pluginErrors].
     */
    data class InvalidPlugin(
        /**
         * [Resource cache entry] [ResourceCacheEntry] that protects the
         * [pluginDetails] until the entry is closed.
         */
        private val resourceCacheEntry: ResourceCacheEntry<PluginDetailsProvider.Result>,

        /**
         * The [errors] [PluginProblem.Level.ERROR] of the plugin
         * that make it invalid. It can also contain the [warnings] [PluginProblem.Level.WARNING]
         * of the plugin's structure.
         */
        val pluginErrors: List<PluginProblem>
    ) : Result() {

      override fun close() = resourceCacheEntry.close()
    }

    /**
     * The [getPluginDetailsCacheEntry] is failed with [error].
     * The presentable reason is [reason].
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