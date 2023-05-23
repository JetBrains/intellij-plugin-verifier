package com.jetbrains.pluginverifier.misc

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import java.lang.IllegalStateException

class MockWebServerUtil

fun MockWebServer.enqueueFromClasspath(classpathResourcePath: String, clazz: Class<*> = MockWebServerUtil::class.java): MockResponse {
  if (!classpathResourcePath.startsWith("/")) {
    throw IllegalArgumentException("Classpath resource must start with a slash '/': [$classpathResourcePath]")
  }

  val resourceStream = clazz
          .getResourceAsStream(classpathResourcePath)
          ?: throw IllegalStateException("Cannot read classpath stream '$classpathResourcePath'")

  val response = MockResponse().setBody(Buffer().readFrom(resourceStream))
  this.enqueue(response)
  return response
}