package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.checkHostIsAvailable
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginRepositoryProperties
import com.jetbrains.pluginverifier.repository.repositories.custom.ExceptionAnalyzerPluginRepository
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
    val url = CustomPluginRepositoryProperties.EXCEPTION_ANALYZER_PLUGIN_REPOSITORY_URL.getUrl()!!
    val sourceCodeUrl = CustomPluginRepositoryProperties.EXCEPTION_ANALYZER_PLUGIN_SOURCE_CODE_URL.getUrl()!!

    val expectedInfos = listOf(
      CustomPluginInfo(
        "com.intellij.sisyphus",
        "ExceptionAnalyzer",
        "IGNORED",
        "JetBrains",
        url,
        URL(url, "/ExceptionAnalyzer.zip"),
        url,
        sourceCodeUrl,
        null,
        null
      )
    )

    CustomPluginRepositoryListingParserTest.assertCustomPluginInfoListsAreTheSame(expectedInfos, repository.getAllVersionsOfPlugin("com.intellij.sisyphus"), checkVersions = false)
    CustomPluginRepositoryListingParserTest.assertCustomPluginInfoListsAreTheSame(expectedInfos, repository.getAllPlugins(), checkVersions = false)
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllVersionsOfPlugin("com.intellij.sisyphus")
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins.first()
    checkDownloadPlugin(pluginInfo)
  }

}
