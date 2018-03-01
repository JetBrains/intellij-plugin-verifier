package com.jetbrains.pluginverifier.tests.cache

import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Closeable

class ResourceCacheTest {

  @Test
  fun `cache functional test`() {
    val N = 10
    val openedResources = hashMapOf<Int, Closeable>()

    /**
     * Create a resource cache
     */
    val resourceCache = createSizeLimitedResourceCache(
        N,
        object : ResourceProvider<Int, Closeable> {
          override fun provide(key: Int): ProvideResult.Provided<Closeable> {
            val closeable = Closeable {
              openedResources.remove(key)
            }
            openedResources[key] = closeable
            return ProvideResult.Provided(
                closeable
            )
          }
        },
        { it.close() },
        "testCache"
    )

    //Populate the cache with keys [0; N)
    for (i in 0 until N) {
      val found = resourceCache.getResourceCacheEntry(i) as ResourceCacheEntryResult.Found
      found.resourceCacheEntry.close()
    }
    assertEquals((0 until N).toSet(), openedResources.keys)

    //Evict the previously inserted elements and populate the cache with keys [N; N * 2)
    for (i in N until 2 * N) {
      val found = resourceCache.getResourceCacheEntry(i) as ResourceCacheEntryResult.Found
      found.resourceCacheEntry.close()
    }
    assertEquals((N until 2 * N).toSet(), openedResources.keys)

    //entries of the resource cache must be closed when no more used
    resourceCache.close()

    assertEquals(emptySet<Int>(), openedResources.keys)
  }
}