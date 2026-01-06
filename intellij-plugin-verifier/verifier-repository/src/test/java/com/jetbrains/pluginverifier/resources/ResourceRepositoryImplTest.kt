/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.resources

import com.jetbrains.pluginverifier.repository.cleanup.SizeEvictionPolicy
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryImpl
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryResult
import org.junit.Assert.*
import org.junit.Test
import java.io.Closeable
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

class ResourceRepositoryImplTest {

  private fun createSizedResourceRepository(
    maximumSize: Int,
    resourceProvider: (Int) -> Closeable
  ) = ResourceRepositoryImpl(
    SizeEvictionPolicy(maximumSize),
    Clock.systemUTC(),
    object : ResourceProvider<Int, Closeable> {
      override fun provide(key: Int) = ProvideResult.Provided(resourceProvider(key))
    },
    SizeWeight(0),
    {
      SizeWeight(1)
    },
    {
      it.close()
    },
    "testRepository"
  )

  @Test
  fun `resource repository with maximum size must release least recently used resources`() {
    val releasedResources = hashSetOf<Int>()
    val repo = createSizedResourceRepository(5) { id ->
      Closeable {
        releasedResources.add(id)
      }
    }

    for (i in 1..10) {
      val result = repo.get(i) as ResourceRepositoryResult.Found
      result.lockedResource.release()
    }

    assertEquals(setOf(1, 2, 3, 4, 5), releasedResources)
  }

  @Test
  fun `getAllExistingKeys must return an immutable copy`() {
    val size = 10
    val resourceRepository = createSizedResourceRepository(size) {
      Closeable { }
    }

    assertEquals(emptySet<Any>(), resourceRepository.getAllExistingKeys())

    //Add the initial keys and unlock the resources immediately
    for (i in 0 until size) {
      val repositoryResult = resourceRepository.get(i) as ResourceRepositoryResult.Found
      repositoryResult.lockedResource.release()
    }

    val existingKeys = resourceRepository.getAllExistingKeys()
    assertEquals(size, existingKeys.size)

    for (i in 0 until size) {
      resourceRepository.remove(i)
    }
    assertEquals((0 until size).toSet(), existingKeys)
  }

  @Test
  fun `removeAll must evict all non-locked resources and schedule for eviction all the locked ones`() {
    val evictedResources = hashSetOf<Int>()
    val size = 10
    val resourceRepository = createSizedResourceRepository(size) { i ->
      Closeable {
        evictedResources.add(i)
      }
    }

    //The first half of the resources must be removed immediately
    //While the second half must be removed after the locks are released
    val resourceLocks = arrayListOf<ResourceLock<Closeable, SizeWeight>>()
    for (i in 0 until size) {
      val repositoryResult = resourceRepository.get(i) as ResourceRepositoryResult.Found
      val lockedResource = repositoryResult.lockedResource
      if (i < size / 2) {
        lockedResource.release()
      } else {
        resourceLocks.add(lockedResource)
      }
    }

    assertEquals(size, resourceRepository.getAllExistingKeys().size)
    resourceRepository.removeAll()
    assertEquals(size / 2, resourceRepository.getAllExistingKeys().size)

    for (resourceLock in resourceLocks) {
      resourceLock.release()
    }
    assertEquals(0, resourceRepository.getAllExistingKeys().size)

    assertEquals((0 until size).toSet(), evictedResources)
  }

  @Test
  fun `resource is not removed until the last lock is released`() {
    val locksCnt = AtomicInteger()
    val resourceRepository = createSizedResourceRepository(1) {
      Closeable {
        if (locksCnt.get() > 0) {
          fail("The resource must not be removed as it has locks registered for it")
        }
      }
    }
    val key = 1
    val lockOne = (resourceRepository.get(key) as ResourceRepositoryResult.Found).lockedResource
    locksCnt.incrementAndGet()

    val lockTwo = (resourceRepository.get(key) as ResourceRepositoryResult.Found).lockedResource
    locksCnt.incrementAndGet()

    resourceRepository.remove(key)

    assertTrue(resourceRepository.has(key))
    locksCnt.decrementAndGet()
    lockOne.release()

    assertTrue(resourceRepository.has(key))
    locksCnt.decrementAndGet()
    lockTwo.release()

    assertFalse(resourceRepository.has(key))
  }

}