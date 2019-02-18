package com.jetbrains.pluginverifier.parameters.jdk

import com.jetbrains.plugin.structure.classes.jdk.JdkResolverCreator
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable

/**
 * Caches the [JdkDescriptor]s for [JdkPath]s.
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
  fun getJdkResolver(path: JdkPath): ResourceCacheEntryResult<JdkDescriptor, SizeWeight> =
      descriptorsCache.getResourceCacheEntry(path)

  override fun close() = descriptorsCache.close()

  private inner class JdkClassesResourceProvider : ResourceProvider<JdkPath, JdkDescriptor> {
    override fun provide(key: JdkPath): ProvideResult<JdkDescriptor> {
      val jdkPath = key.jdkPath
      val resolver = try {
        JdkResolverCreator.createJdkResolver(jdkPath.toFile())
      } catch (ie: InterruptedException) {
        throw ie
      } catch (e: Exception) {
        return ProvideResult.Failed("Failed to read JDK classes $key: $jdkPath", e)
      }
      return ProvideResult.Provided(JdkDescriptor(resolver, jdkPath))
    }
  }

}