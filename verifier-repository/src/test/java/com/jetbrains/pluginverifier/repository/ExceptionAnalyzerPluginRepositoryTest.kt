package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.repository.repositories.custom.ExceptionAnalyzerPluginRepository
import com.jetbrains.pluginverifier.results.HostReachableRule
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URL

@HostReachableRule.HostReachable("https://google.com")
class ExceptionAnalyzerPluginRepositoryTest : BaseRepositoryTest<ExceptionAnalyzerPluginRepository>() {

  companion object {
    val eaWebUrl = URL("https://ea-engine.labs.intellij.net")
  }

  override fun createRepository() = ExceptionAnalyzerPluginRepository()

  @Test
  fun `verify plugin info`() {
    val allPlugins = repository.getAllPlugins()
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins[0]
    assertEquals("com.intellij.sisyphus", pluginInfo.pluginId)
    assertEquals("ExceptionAnalyzer", pluginInfo.pluginName)
    assertEquals("JetBrains", pluginInfo.vendor)
    assertEquals(null, pluginInfo.sinceBuild)
    assertEquals(null, pluginInfo.untilBuild)
    assertEquals(URL(eaWebUrl, "/ExceptionAnalyzer.zip"), pluginInfo.downloadUrl)
    assertEquals(eaWebUrl, pluginInfo.browserUrl)
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllPlugins()
    Assert.assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins.first()
    checkDownloadPlugin(pluginInfo)
  }

}
