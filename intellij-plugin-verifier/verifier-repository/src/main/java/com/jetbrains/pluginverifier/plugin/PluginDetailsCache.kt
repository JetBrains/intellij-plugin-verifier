package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import java.io.Closeable

/**
 * This cache is intended to open and cache [PluginDetails] for
 * use by multiple threads. It is necessary because the details creation may be expensive
 * as it requires downloading the plugin, reading its class files and registering a file lock.
 *
 * The cache must be [closed][close] on the application shutdown to free all the details.
 */
interface PluginDetailsCache : Closeable {
  /**
   * Provides the [PluginDetails] of the given [pluginInfo] wrapped in a [Result].
   */
  fun getPluginDetailsCacheEntry(pluginInfo: PluginInfo): Result

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