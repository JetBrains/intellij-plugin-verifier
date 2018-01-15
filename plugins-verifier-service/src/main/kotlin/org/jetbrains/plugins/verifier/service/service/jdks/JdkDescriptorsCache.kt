package org.jetbrains.plugins.verifier.service.service.jdks

import com.jetbrains.plugin.structure.classes.jdk.JdkResolverCreator
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.repository.cache.ResourceCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable
import java.nio.file.Path

/**
 * Caches the [JdkDescriptor]s for [JdkVersion]s.
 */
class JdkDescriptorsCache(private val jdk8path: Path) : Closeable {

  /**
   * Provides a path to the JDK by specified [version].
   */
  private fun getJdkHome(version: JdkVersion) = when (version) {
    JdkVersion.JAVA_8_ORACLE -> jdk8path
  }

  private val resourceCache = ResourceCache(
      3,
      JdkClassesResourceProvider(),
      { it.close() },
      "JDKCache"
  )

  /**
   * Provides a [JdkDescriptor] for the [version]
   * wrapped in a [ResourceCacheEntryResult]
   * that protects the [JdkDescriptor] from eviction
   * from the cache until the entry is closed.
   */
  fun getJdkResolver(version: JdkVersion): ResourceCacheEntryResult<JdkDescriptor> =
      resourceCache.getResourceCacheEntry(version)

  override fun close() = resourceCache.close()

  private inner class JdkClassesResourceProvider : ResourceProvider<JdkVersion, JdkDescriptor> {
    override fun provide(key: JdkVersion): ProvideResult<JdkDescriptor> {
      val jdkPath = getJdkHome(key)
      val resolver = try {
        JdkResolverCreator.createJdkResolver(jdkPath.toFile())
      } catch (e: Exception) {
        return ProvideResult.Failed("Failed to read JDK classes $key: $jdkPath", e)
      }
      return ProvideResult.Provided(JdkDescriptor(resolver, jdkPath))
    }
  }

}