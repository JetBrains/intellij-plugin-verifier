package com.jetbrains.plugin.structure.teamcity.mock

import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import com.jetbrains.plugin.structure.teamcity.TeamcityVersion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TeamcityMockPluginsTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private fun testMockConfigs(plugin: TeamcityPlugin) {
    assertEquals(null, plugin.url)
    assertEquals("Google Compute Engine cloud integration", plugin.pluginName)
    assertEquals("@Version@", plugin.pluginVersion)
    assertEquals("teamcity_cloud-google", plugin.pluginId)

    assertEquals("email@example.org", plugin.vendorEmail)
    assertEquals("https://www.jetbrains.com/", plugin.vendorUrl)
    assertEquals("JetBrains, s.r.o.", plugin.vendor)

    assertEquals("Support for build agents running on Google Cloud", plugin.description)
    assertEquals(TeamcityVersion(2), plugin.sinceBuild)
    assertEquals(TeamcityVersion(3), plugin.untilBuild)

    assertEquals(null, plugin.changeNotes)

    assertEquals("https://github.com/Jetbrains/teamcity-google-agent", plugin.downloadUrl)
    assertTrue(plugin.useSeparateClassLoader)

    assertEquals(1, plugin.parameters?.size)
    assertEquals("value", plugin.parameters?.get("key"))
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    assertTrue(problems.isEmpty())
  }

  private fun getMockPluginXmlContent(): String {
    return this::class.java.getResource("/teamcity/teamcity-plugin.xml").readText()
  }

  @Test
  fun `plugin packed in zip`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file("teamcity-plugin.xml", getMockPluginXmlContent())
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  @Test
  fun `plugin directory`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file("teamcity-plugin.xml", getMockPluginXmlContent())
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: File) {
    val pluginCreationResult = TeamcityPluginManager.createManager().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess
    val plugin = pluginCreationSuccess.plugin

    testMockConfigs(plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

}
