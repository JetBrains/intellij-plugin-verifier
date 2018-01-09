package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache.Result.Provided
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable

/**
 * This cache is intended to open and cache [PluginDetails] for
 * use by multiple threads. It is necessary because the details creation may be expensive
 * as it requires downloading the plugin, reading its class files and registering a file lock.
 *
 * The cache must be [closed] [close] on the application shutdown to free all the details.
 */
class PluginDetailsCache(cacheSize: Int, pluginDetailsProvider: PluginDetailsProvider) : Closeable {

  private val resourceCache = ResourceCache(
      cacheSize.toLong(),
      PluginDetailsResourceProvider(pluginDetailsProvider),
      { it.close() },
      "PluginDetailsCache"
  )

  /**
   * Provides the [PluginDetails] of the given [pluginInfo].
   * The possible results are represented as instances of [Result].
   */
  fun getPluginDetails(pluginInfo: PluginInfo): Result {
    return with(resourceCache.getResourceCacheEntry(pluginInfo)) {
      when (this) {
        is ResourceCacheEntryResult.Found -> with(resourceCacheEntry.resource) {
          when (this) {
            is PluginDetailsProvider.Result.Provided -> Result.Provided(this)
            is PluginDetailsProvider.Result.InvalidPlugin -> Result.InvalidPlugin(this)
          }
        }
        is ResourceCacheEntryResult.Failed -> Result.Failed(message, error)
        is ResourceCacheEntryResult.NotFound -> Result.FileNotFound(message)
      }
    }
  }

  override fun close() = resourceCache.close()

  /**
   * Represents possible results of the [getPluginDetails].
   * It must be closed after usage to deallocate the [Provided.resourceCacheEntry].
   */
  sealed class Result : Closeable {

    /**
     * The [pluginDetails] are successfully provided.
     */
    data class Provided(
        /**
         * [Resource cache entry] [ResourceCacheEntry] that protects the
         * [pluginDetails] until the entry is closed.
         */
        private val resourceCacheEntry: PluginDetailsProvider.Result.Provided
    ) : Result() {

      /**
       * The provided [PluginDetails].
       * It will be closed when [_this_] [Result.Provided] is closed.
       */
      val pluginDetails: PluginDetails
        get() = resourceCacheEntry.pluginDetails

      override fun close() = resourceCacheEntry.close()
    }

    /**
     * The [PluginDetails] are not provided because the plugin
     * passed to [getPluginDetails] is [invalid] [pluginErrors].
     */
    data class InvalidPlugin(
        /**
         * [Resource cache entry] [ResourceCacheEntry] that protects the
         * [pluginDetails] until the entry is closed.
         */
        private val resourceCacheEntry: PluginDetailsProvider.Result.InvalidPlugin
    ) : Result() {

      /**
       * The [errors] [PluginProblem.Level.ERROR] of the plugin
       * that make it invalid. It can also contain the [warnings] [PluginProblem.Level.WARNING]
       * of the plugin's structure.
       */
      val pluginErrors: List<PluginProblem>
        get() = resourceCacheEntry.pluginErrors

      override fun close() = Unit
    }

    /**
     * The [getPluginDetails] is failed with [error].
     * The presentable reason is [reason].
     */
    data class Failed(val reason: String, val error: Throwable) : Result() {
      override fun close() = Unit
    }

    /**
     * The [PluginDetails] are not provided because the file
     * of the plugin passed to [getPluginDetails] is not found.
     */
    data class FileNotFound(val reason: String) : Result() {
      override fun close() = Unit
    }
  }

  /**
   * The bridge class that friends the [ResourceProvider] and [PluginDetailsProvider].
   */
  private class PluginDetailsResourceProvider(private val pluginDetailsProvider: PluginDetailsProvider) : ResourceProvider<PluginInfo, PluginDetailsProvider.Result> {

    override fun provide(key: PluginInfo): ProvideResult<PluginDetailsProvider.Result> {
      return with(key.pluginRepository.downloadPluginFile(key)) {
        when (this) {
          is FileRepositoryResult.Found -> ProvideResult.Provided(pluginDetailsProvider.providePluginDetails(key, lockedFile))
          is FileRepositoryResult.NotFound -> ProvideResult.NotFound(reason)
          is FileRepositoryResult.Failed -> ProvideResult.Failed(reason, error)
        }
      }
    }

  }

}