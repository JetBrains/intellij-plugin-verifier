package com.jetbrains.pluginverifier.ide.repositories

import com.jetbrains.pluginverifier.ide.repositories.IntelliJIdeRepository.Channel.RELEASE
import com.jetbrains.pluginverifier.misc.enqueueFromClasspath
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import org.junit.Test

class IntelliJIdeRepositoryTest {
  @Test
  fun testFetchIndexFromLocalIndex() {
    MockWebServer().use { server ->
      server.enqueueFromClasspath("/intelliJArtifactsRepositoryIndex.json")
      server.start()
      val baseIndexUrl = server.url("").toUrl().toExternalForm()
      val repository = MockIntelliJIdeRepository(baseIndexUrl)
      val availableIdes = repository.fetchIndex()
      Assert.assertEquals(6, availableIdes.size)
    }
  }

  @Test
  fun `IntelliJ Artifacts Repository Index is retrieved from mock server and cached`() {
    MockWebServer().use { server ->
      server.enqueueFromClasspath("/intelliJArtifactsRepositoryIndex.json")
      server.start()
      val baseIndexUrl = server.url("").toUrl().toExternalForm()
      val repository = MockIntelliJIdeRepository(baseIndexUrl)

      // first call, should invoke request
      repository.fetchIndex()
      // second call, should go to the cache
      repository.fetchIndex()
      // thirds call, should go to the cache as well
      repository.fetchIndex()

      Assert.assertEquals(1, server.requestCount)
    }
  }

  class MockIntelliJIdeRepository(private val baseUrl: String) : IntelliJIdeRepository(RELEASE) {
    override val indexBaseUrl: String
      get() = baseUrl
  }
}