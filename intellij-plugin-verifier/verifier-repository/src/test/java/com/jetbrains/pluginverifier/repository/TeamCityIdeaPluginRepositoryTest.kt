package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.checkHostIsAvailable
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginRepositoryProperties
import com.jetbrains.pluginverifier.repository.repositories.custom.TeamCityIdeaPluginRepository
import org.junit.Assert.assertEquals
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
    val allPlugins = repository.getAllPlugins()
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins[0]
    assertEquals("Jetbrains TeamCity Plugin", pluginInfo.pluginId)
    assertEquals("TeamCity Integration", pluginInfo.pluginName)
    assertEquals("JetBrains", pluginInfo.vendor)
    assertEquals(null, pluginInfo.sinceBuild)
    assertEquals(null, pluginInfo.untilBuild)
    val buildServerUrl = CustomPluginRepositoryProperties.TEAM_CITY_PLUGIN_BUILD_SERVER_URL.getUrl()
    assertEquals(URL(buildServerUrl, "/update/TeamCity-IDEAplugin.zip"), pluginInfo.downloadUrl)
    assertEquals(buildServerUrl, pluginInfo.browserUrl)
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllPlugins()
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins.first()
    checkDownloadPlugin(pluginInfo)
  }

  @Test
  fun `parse plugins list for TeamCity Integration IDEA plugin`() {
    val document = parseXmlDocument(
        """
      <plugins>
      <plugin id="Jetbrains TeamCity Plugin" url="TeamCity-IDEAplugin.zip" version="2018.1.58183"/>
      </plugins>
          """.trimIndent()
    )
    val placeholderUrl = URL("https://placeholder.com")
    val list = TeamCityIdeaPluginRepository.parsePluginsList(document, placeholderUrl, placeholderUrl)
    assertEquals(1, list.size)
    val pluginInfo = list[0]
    assertEquals("Jetbrains TeamCity Plugin", pluginInfo.pluginId)
    assertEquals("2018.1.58183", pluginInfo.version)
    assertEquals(URL(placeholderUrl, "/update/TeamCity-IDEAplugin.zip"), pluginInfo.downloadUrl)
    assertEquals(placeholderUrl, pluginInfo.browserUrl)
  }

}
