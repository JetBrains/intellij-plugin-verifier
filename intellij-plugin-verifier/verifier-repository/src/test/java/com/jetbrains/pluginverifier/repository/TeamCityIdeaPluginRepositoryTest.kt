package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.checkHostIsAvailable
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginRepositoryProperties
import com.jetbrains.pluginverifier.repository.repositories.custom.TeamCityIdeaPluginRepository
import org.junit.Assert.assertFalse
import org.junit.Assume
import org.junit.Test
import java.net.URL

class TeamCityIdeaPluginRepositoryTest : BaseRepositoryTest<TeamCityIdeaPluginRepository>() {

  override fun createRepository(): TeamCityIdeaPluginRepository {
    val buildServerUrl = CustomPluginRepositoryProperties.TEAM_CITY_PLUGIN_BUILD_SERVER_URL.getUrl()
    val sourceCodeUrl = CustomPluginRepositoryProperties.TEAM_CITY_PLUGIN_SOURCE_CODE_URL.getUrl()

    Assume.assumeNotNull(buildServerUrl)
    Assume.assumeNotNull(sourceCodeUrl)

    val loginUrl = URL(buildServerUrl, "login.html")

    Assume.assumeTrue(checkHostIsAvailable(loginUrl))
    Assume.assumeTrue(checkHostIsAvailable(sourceCodeUrl!!))

    return TeamCityIdeaPluginRepository(buildServerUrl!!, sourceCodeUrl)
  }

  @Test
  fun `verify plugin info`() {
    val buildServerUrl = CustomPluginRepositoryProperties.TEAM_CITY_PLUGIN_BUILD_SERVER_URL.getUrl()!!
    val sourceCodeUrl = CustomPluginRepositoryProperties.TEAM_CITY_PLUGIN_SOURCE_CODE_URL.getUrl()!!
    val expectedInfos = listOf(
      CustomPluginInfo(
        "Jetbrains TeamCity Plugin",
        "TeamCity Integration",
        "IGNORED",
        "JetBrains",
        buildServerUrl,
        URL(buildServerUrl, "/update/TeamCity-IDEAplugin.zip"),
        buildServerUrl,
        sourceCodeUrl,
        null,
        null
      )
    )

    CustomPluginRepositoryListingParserTest.assertCustomPluginInfoListsAreTheSame(expectedInfos, repository.getAllVersionsOfPlugin("Jetbrains TeamCity Plugin"), checkVersions = false)
    CustomPluginRepositoryListingParserTest.assertCustomPluginInfoListsAreTheSame(expectedInfos, repository.getAllPlugins(), checkVersions = false)
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllVersionsOfPlugin("Jetbrains TeamCity Plugin")
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins.first()
    checkDownloadPlugin(pluginInfo)
  }
}
