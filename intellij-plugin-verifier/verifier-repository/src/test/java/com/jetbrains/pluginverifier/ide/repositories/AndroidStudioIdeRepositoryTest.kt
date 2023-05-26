package com.jetbrains.pluginverifier.ide.repositories

import com.jetbrains.pluginverifier.misc.enqueueFromClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test


class AndroidStudioIdeRepositoryTest {
  @Test
  fun `Android Studio IDES are retrieved from mock server`() {
    MockWebServer().use { server ->
      server.enqueueFromClasspath("/android-studio.feed.xz.signed")
      server.start()

      val baseUrl = server.url("").toUrl().toExternalForm().removeSuffix("/")

      val repository = AndroidStudioIdeRepository(baseUrl)
      val availableIdes = repository.fetchIndex()
      assertEquals(3, availableIdes.size)
      val ai222 = availableIdes[0]
      val ai223 = availableIdes[1]
      val ai231 = availableIdes[2]
      assertEquals("AI-222.4459.24.2221.9971841", ai222.version.asString())
      assertEquals("AI-223.8836.35.2231.10075884", ai223.version.asString())
      assertEquals("AI-231.7864.76.2311.10114981", ai231.version.asString())

      val recordedReq = server.takeRequest()
      assertEquals("/v1/android-studio.feed.xz.signed", recordedReq.path)
    }
  }

  @Test
  fun `Android Studio IDES are retrieved from mock server and cached`() {
    MockWebServer().use { server ->
      server.enqueueFromClasspath("/android-studio.feed.xz.signed")
      server.start()

      val baseUrl = server.url("").toUrl().toExternalForm().removeSuffix("/")

      val repository = AndroidStudioIdeRepository(baseUrl)
      // first call, should invoke request
      repository.fetchIndex()
      // second call, should go to the cache
      repository.fetchIndex()
      // thirds call, should go to the cache as well
      repository.fetchIndex()

      assertEquals(1, server.requestCount)
    }
  }
}