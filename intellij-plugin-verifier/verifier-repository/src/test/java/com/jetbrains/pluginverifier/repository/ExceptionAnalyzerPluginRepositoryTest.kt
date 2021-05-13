package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.checkHostIsAvailable
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginRepositoryProperties
import com.jetbrains.pluginverifier.repository.repositories.custom.ExceptionAnalyzerPluginRepository
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assume
import org.junit.Ignore
import org.junit.Test
import java.net.URL

@Ignore
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
    val url = CustomPluginRepositoryProperties.EXCEPTION_ANALYZER_PLUGIN_REPOSITORY_URL.getUrl()!!
    val sourceCodeUrl = CustomPluginRepositoryProperties.EXCEPTION_ANALYZER_PLUGIN_SOURCE_CODE_URL.getUrl()!!
    for (list in listOf(repository.getAllPlugins(), repository.getAllVersionsOfPlugin("com.intellij.sisyphus"))) {
      for (plugin in list) {
        Assert.assertEquals("com.intellij.sisyphus", plugin.pluginId)
        Assert.assertEquals("ExceptionAnalyzer", plugin.pluginName)
        Assert.assertEquals("IGNORED", plugin.version)
        Assert.assertEquals("JetBrains", plugin.vendor)
        Assert.assertEquals(url, plugin.repositoryUrl)
        Assert.assertEquals(URL(url, "/ExceptionAnalyzer.zip"), plugin.downloadUrl)
        Assert.assertEquals(url, plugin.browserUrl)
        Assert.assertEquals(sourceCodeUrl, plugin.sourceCodeUrl)
        Assert.assertEquals(null, plugin.sinceBuild)
        Assert.assertEquals(null, plugin.untilBuild)
      }
    }
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllVersionsOfPlugin("com.intellij.sisyphus")
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins.first()
    checkDownloadPlugin(pluginInfo)
  }

}
