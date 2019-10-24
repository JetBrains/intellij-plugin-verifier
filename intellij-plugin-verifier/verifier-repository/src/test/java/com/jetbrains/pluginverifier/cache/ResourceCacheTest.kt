package com.jetbrains.pluginverifier.cache

import com.jetbrains.pluginverifier.repository.cache.ResourceCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Closeable
import java.util.*

/**
 * Verifies correctness of [ResourceCache] implementation.
 */
class ResourceCacheTest {

  /**
   * Populates a [ResourceCache] with limited size in a single thread,
   * verifies that all resources are disposed at proper time:
   * 1) If there is space in the cache, don't dispose resources for no purpose.
   * 2) If the space is out, dispose the least used resources
   * 3) When closing the cache, dispose all the resources.
   */
  @Test
  fun `cache resource disposition test`() {
    val elementsNum = 10
    val openedResources = Collections.synchronizedMap(hashMapOf<Int, Closeable>())

    val resourceCache = createSizeLimitedResourceCache(
      elementsNum,
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
    for (i in 0 until elementsNum) {
      val found = resourceCache.getResourceCacheEntry(i) as ResourceCacheEntryResult.Found
      found.resourceCacheEntry.close()
    }
    //Resources [0; N) must not be closed yet because there is enough space for all of them
    assertEquals((0 until elementsNum).toSet(), openedResources.keys)

    //Evict the previously inserted elements and populate the cache with keys [N; N * 2)
    for (i in elementsNum until 2 * elementsNum) {
      val found = resourceCache.getResourceCacheEntry(i) as ResourceCacheEntryResult.Found
      found.resourceCacheEntry.close()
    }
    assertEquals((elementsNum until 2 * elementsNum).toSet(), openedResources.keys)

    //entries of the resource cache must be closed when no more used
    resourceCache.close()

    assertEquals(emptySet<Int>(), openedResources.keys)
  }
}