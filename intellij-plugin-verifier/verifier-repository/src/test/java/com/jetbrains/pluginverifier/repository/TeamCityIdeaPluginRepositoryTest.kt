package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginRepositoryProperties
import com.jetbrains.pluginverifier.repository.repositories.custom.TeamCityIdeaPluginRepository
import org.junit.Assert
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

//    Assume.assumeTrue(checkHostIsAvailable(loginUrl))
//    Assume.assumeTrue(checkHostIsAvailable(sourceCodeUrl!!))

    return TeamCityIdeaPluginRepository(buildServerUrl!!, sourceCodeUrl!!)
  }

  @Test
  fun `verify plugin info`() {
    val buildServerUrl = CustomPluginRepositoryProperties.TEAM_CITY_PLUGIN_BUILD_SERVER_URL.getUrl()!!
    val sourceCodeUrl = CustomPluginRepositoryProperties.TEAM_CITY_PLUGIN_SOURCE_CODE_URL.getUrl()!!
    for (list in listOf(repository.getAllPlugins(), repository.getAllVersionsOfPlugin("Jetbrains TeamCity Plugin"))) {
      for (plugin in list) {
        Assert.assertEquals("Jetbrains TeamCity Plugin", plugin.pluginId)
        Assert.assertEquals("TeamCity Integration", plugin.pluginName)
        Assert.assertEquals("JetBrains", plugin.vendor)
        Assert.assertEquals(buildServerUrl, plugin.repositoryUrl)
        Assert.assertEquals(URL(buildServerUrl, "/update/TeamCity-IDEAplugin.zip"), plugin.downloadUrl)
        Assert.assertEquals(buildServerUrl, plugin.browserUrl)
        Assert.assertEquals(sourceCodeUrl, plugin.sourceCodeUrl)
        Assert.assertEquals(null, plugin.sinceBuild)
        Assert.assertEquals(null, plugin.untilBuild)
      }
    }
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllVersionsOfPlugin("Jetbrains TeamCity Plugin")
    assertFalse(allPlugins.isEmpty())
    val pluginInfo = allPlugins.first()
    checkDownloadPlugin(pluginInfo)
  }
}
