package com.jetbrains.pluginverifier.ide.repositories

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedReader
import java.lang.IllegalStateException

class ReleaseIdeRepositoryTest {
  @Test
  fun `IDEs are retrieved from the server`() {
    val server = MockWebServer()

    val jsonStream = ReleaseIdeRepositoryTest::class.java
            .getResourceAsStream("/releaseIdeRepositoryIndex-IIU.json")
            ?: throw IllegalStateException("Cannot read index from JSON in classpath")

    val jsonContent = jsonStream
            .bufferedReader()
            .use(BufferedReader::readText)

    val response = MockResponse().setBody(jsonContent)
    server.enqueue(response)
    server.start()

    val baseUrl = server.url("/")

    val repository = ReleaseIdeRepository(baseUrl.toString())
    val availableIdes = repository.fetchIndex()

    assertEquals(2, availableIdes.size)
    assertEquals("IU-232.6095.10", availableIdes[0].version.asString())
    assertEquals("IU-232.5150.116", availableIdes[1].version.asString())

    server.shutdown()
  }
}