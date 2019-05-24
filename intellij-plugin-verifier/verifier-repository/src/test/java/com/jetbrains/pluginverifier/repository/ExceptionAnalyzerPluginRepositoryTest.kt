package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.checkHostIsAvailable
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginRepositoryProperties
import com.jetbrains.pluginverifier.repository.repositories.custom.ExceptionAnalyzerPluginRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume
import org.junit.Test
import java.net.URL

class ExceptionAnalyzerPluginRepositoryTest : BaseRepositoryTest<ExceptionAnalyzerPluginRepository>() {

  override fun createRepository(): ExceptionAnalyzerPluginRepository {
    val repositoryUrl = CustomPluginRepositoryProperties.EXCEPTION_ANALYZER_PLUGIN_REPOSITORY_URL.getUrl()
    val sourceCodeUrl = CustomPluginRepositoryProperties.EXCEPTION_ANALYZER_PLUGIN_SOURCE_CODE_URL.getUrl()

    Assume.assumeNotNull(repositoryUrl)
    Assume.assumeNotNull(sourceCodeUrl)

    Assume.assumeTrue(checkHostIsAvailable(repositoryUrl!!))
    Assume.assumeTrue(checkHostIsAvailable(sourceCodeUrl!!))

    return ExceptionAnalyzerPluginRepository(repositoryUrl, sourceCodeUrl)
  }

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
    val url = CustomPluginRepositoryProperties.EXCEPTION_ANALYZER_PLUGIN_REPOSITORY_URL.getUrl()
    assertEquals(URL(url, "/ExceptionAnalyzer.zip"), pluginInfo.downloadUrl)
    assertEquals(url, pluginInfo.browserUrl)
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllPlugins()
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins.first()
    checkDownloadPlugin(pluginInfo)
  }

}
