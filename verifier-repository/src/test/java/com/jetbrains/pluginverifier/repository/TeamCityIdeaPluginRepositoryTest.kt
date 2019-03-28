package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.repository.repositories.custom.TeamCityIdeaPluginRepository
import com.jetbrains.pluginverifier.results.HostReachableRule
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URL

@HostReachableRule.HostReachable("https://buildserver.labs.intellij.net")
class TeamCityIdeaPluginRepositoryTest : BaseRepositoryTest<TeamCityIdeaPluginRepository>() {

  companion object {
    val buildServerUrl = URL("https://buildserver.labs.intellij.net")
  }

  override fun createRepository() = TeamCityIdeaPluginRepository(buildServerUrl)

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
    assertEquals(URL(buildServerUrl, "/update/TeamCity-IDEAplugin.zip"), pluginInfo.downloadUrl)
    assertEquals(buildServerUrl, pluginInfo.browserUrl)
  }

  @Test
  fun `download some plugin`() {
    val allPlugins = repository.getAllPlugins()
    Assert.assertFalse(allPlugins.isEmpty())
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
    val list = TeamCityIdeaPluginRepository.parsePluginsList(document, buildServerUrl)
    assertEquals(1, list.size)
    val pluginInfo = list[0]
    assertEquals("Jetbrains TeamCity Plugin", pluginInfo.pluginId)
    assertEquals("2018.1.58183", pluginInfo.version)
    assertEquals(URL(buildServerUrl, "/update/TeamCity-IDEAplugin.zip"), pluginInfo.downloadUrl)
    assertEquals(buildServerUrl, pluginInfo.browserUrl)
  }

}
