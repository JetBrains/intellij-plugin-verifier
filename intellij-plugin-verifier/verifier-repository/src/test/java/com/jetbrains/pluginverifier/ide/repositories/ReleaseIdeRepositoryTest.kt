package com.jetbrains.pluginverifier.ide.repositories

import com.jetbrains.pluginverifier.misc.enqueueFromClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedReader
import java.lang.IllegalStateException

class ReleaseIdeRepositoryTest {
  @Test
  fun `IDEs are retrieved from the server`() {
    MockWebServer().use { server ->
      server.enqueueFromClasspath("/releaseIdeRepositoryIndex-IIU.json")
      server.start()

      val baseUrl = server.url("/")

      val repository = ReleaseIdeRepository(baseUrl.toString())
      val availableIdes = repository.fetchIndex()

      assertEquals(2, availableIdes.size)
      assertEquals("IU-232.6095.10", availableIdes[0].version.asString())
      assertEquals("IU-232.5150.116", availableIdes[1].version.asString())
    }
  }
}