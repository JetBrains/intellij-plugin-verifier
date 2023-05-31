package com.jetbrains.pluginverifier.misc

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers


class RestApisKtTest {
  @Test
  fun `redirect is handled properly`() {
    val redirectSourceSuffix = "/redirect-source"
    val redirectTargetSuffix = "/redirect-target"

    MockWebServer().use { server ->
      val dispatcher = dispatcher {
        when (it.path) {
          redirectSourceSuffix -> MockResponse().setResponseCode(302).setHeader("Location", "/redirect-target")
          "/redirect-target" -> MockResponse().setResponseCode(200)
          else -> MockResponse().setResponseCode(404)
        }
      }
      server.dispatcher = dispatcher
      val baseUri = server.url("/").toUrl().toURI()

      val redirectSourceUri = baseUri.resolve(redirectSourceSuffix)
      val redirectTargetUri = baseUri.resolve(redirectTargetSuffix)

      val request = HttpRequest.newBuilder().GET().uri(redirectSourceUri).build()

      val http = createHttpClient()
      val response = http.send(request, BodyHandlers.ofString())
      assertEquals(200, response.statusCode())
      assertEquals(redirectTargetUri, response.request().uri())
    }
  }

  private fun dispatcher(dispatchRules: (RecordedRequest) -> MockResponse) = object : Dispatcher() {
    @Throws(InterruptedException::class)
    override fun dispatch(request: RecordedRequest): MockResponse = dispatchRules.invoke(request)
  }
}