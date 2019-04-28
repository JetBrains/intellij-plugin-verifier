package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable

/**
 * Cache of [IdeDescriptor] associated by [IdeVersion]s.
 *
 * This must be [closed] [close] on the application shutdown
 * to deallocate all the [IdeDescriptor]s.
 */
class IdeDescriptorsCache(
    cacheSize: Int,
    ideFilesBank: IdeFilesBank
) : Closeable {

  private val descriptorsCache = createSizeLimitedResourceCache(
      cacheSize,
      IdeDescriptorResourceProvider(ideFilesBank),
      { it.close() },
      "IdeDescriptorsCache"
  )

  /**
   * Atomically creates an [IdeDescriptor] for IDE [ideVersion]
   * and registers a [ResourceCacheEntry] for it.
   * The cache's state is not modified until this method returns.
   */
  @Throws(InterruptedException::class)
  fun getIdeDescriptorCacheEntry(ideVersion: IdeVersion): Result {
    val resourceCacheEntryResult = descriptorsCache.getResourceCacheEntry(ideVersion)
    return with(resourceCacheEntryResult) {
      when (this) {
        is ResourceCacheEntryResult.Found -> Result.Found(resourceCacheEntry)
        is ResourceCacheEntryResult.Failed -> Result.Failed(message, error)
        is ResourceCacheEntryResult.NotFound -> Result.NotFound(message)
      }
    }
  }

  /**
   * Result of [fetching] [getIdeDescriptorCacheEntry] an entry from this cache.
   */
  sealed class Result : Closeable {
    /**
     * Resource [entry] [resourceCacheEntry] has been fetched.
     */
    data class Found(private val resourceCacheEntry: ResourceCacheEntry<IdeDescriptor, SizeWeight>) : Result() {

      val ideDescriptor: IdeDescriptor
        get() = resourceCacheEntry.resource

      override fun close() = resourceCacheEntry.close()
    }

    /**
     * Resource is not fetched because [error] was thrown.
     * Investigate [reason] for a human-readable message.
     */
    data class Failed(val reason: String, val error: Throwable) : Result() {
      override fun close() = Unit
    }

    /**
     * Resource is not found because of [reason].
     */
    data class NotFound(val reason: String) : Result() {
      override fun close() = Unit
    }
  }

  /**
   * Implementation of the [ResourceProvider] that provides the IDE files from the [ideFilesBank].
   */
  private class IdeDescriptorResourceProvider(private val ideFilesBank: IdeFilesBank) : ResourceProvider<IdeVersion, IdeDescriptor> {

    override fun provide(key: IdeVersion): ProvideResult<IdeDescriptor> {
      val result = ideFilesBank.getIdeFile(key)
      val ideLock = (result as? IdeFilesBank.Result.Found)?.ideFileLock
          ?: return ProvideResult.NotFound("IDE $key is not found in the $ideFilesBank")
      val ideDescriptor = try {
        IdeDescriptor.create(ideLock.file, key, ideLock)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        return ProvideResult.Failed("Unable to open IDE $key", e)
      }
      return ProvideResult.Provided(ideDescriptor)
    }
  }

  override fun close() = descriptorsCache.close()
}