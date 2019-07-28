package com.jetbrains.pluginverifier.jdk

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable
import java.nio.file.Path

/**
 * Caches created [JdkDescriptor]s.
 */
class JdkDescriptorsCache : Closeable {

  private val descriptorsCache = createSizeLimitedResourceCache(
      3,
      JdkClassesResourceProvider(),
      { it.close() },
      "JDKCache"
  )

  /**
   * Provides a [JdkDescriptor] for the [path]
   * wrapped in a [ResourceCacheEntryResult]
   * that protects the [JdkDescriptor] from eviction
   * from the cache until the entry is closed.
   */
  fun getJdkResolver(path: Path): ResourceCacheEntryResult<JdkDescriptor, SizeWeight> =
      descriptorsCache.getResourceCacheEntry(path)

  override fun close() = descriptorsCache.close()

  private inner class JdkClassesResourceProvider : ResourceProvider<Path, JdkDescriptor> {
    override fun provide(key: Path): ProvideResult<JdkDescriptor> {
      return try {
        val jdkDescriptor = JdkDescriptorCreator.createJdkDescriptor(key)
        ProvideResult.Provided(jdkDescriptor)
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        return ProvideResult.Failed("Failed to read JDK classes $key: $key", e)
      }
    }
  }

}