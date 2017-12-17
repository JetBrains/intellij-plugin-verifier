package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.SizeEvictionPolicy
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryImpl
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Closeable
import java.time.Clock

class ResourceRepositoryImplTest {

  private fun createSizedResourceRepository(maximumSize: Long,
                                            resourceProvider: (Int) -> Closeable) = ResourceRepositoryImpl(
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
      }
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
      println(repo)
    }

    assertEquals(setOf(1, 2, 3, 4, 5), releasedResources)
  }
}