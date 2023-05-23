package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.repository.CustomPluginRepositoryListingType
import com.jetbrains.pluginverifier.misc.enqueueFromClasspath
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class DefaultCustomPluginRepositoryTest {
  @Test
  fun `download from repository is successful`() {
    MockWebServer().use { server ->
      server.enqueueFromClasspath("/plugins.xml")
      server.start()

      val serverUrl = server.url("/").toUrl()
      val pluginsListXmlUrl = serverUrl
      val repositoryUrl = URL("http://unittest.example.com/repository")
      // matches format of 'plugins.xml' from mock response
      val pluginsXmlListingType = CustomPluginRepositoryListingType.PLUGIN_REPOSITORY
      val repository = DefaultCustomPluginRepository(repositoryUrl,
              pluginsListXmlUrl,
              pluginsXmlListingType,
              "Integration Test (JUnit) Plugin Repository")

      val allPlugins = repository.requestAllPlugins()
      assertEquals(1, allPlugins.size)
      val plugin = allPlugins[0]
      assertEquals(URL("http://unittest.example.com/repository"), plugin.repositoryUrl)
      assertEquals(URL("http://unittest.example.com/repository"), plugin.browserUrl)
      assertEquals(URL("${serverUrl}file.zip"), plugin.downloadUrl)
    }
  }

  @Test
  fun `download from repository fails with 404`() {
    MockWebServer().use { server ->
      server.enqueue(MockResponse().setResponseCode(404))
      server.start()

      val serverUrl = server.url("/").toUrl()
      val pluginsListXmlUrl = serverUrl
      val repositoryUrl = URL("http://unittest.example.com/repository")
      // matches format of 'plugins.xml' from mock response
      val pluginsXmlListingType = CustomPluginRepositoryListingType.PLUGIN_REPOSITORY
      val repository = DefaultCustomPluginRepository(repositoryUrl,
              pluginsListXmlUrl,
              pluginsXmlListingType,
              "Integration Test (JUnit) Plugin Repository")

      val allPlugins = repository.requestAllPlugins()
      assertEquals(0, allPlugins.size)
    }
  }
}