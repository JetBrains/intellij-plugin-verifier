package com.jetbrains.plugin.structure.teamcity.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.mocks.BaseMockPluginTest
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import com.jetbrains.plugin.structure.teamcity.TeamcityVersion
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TeamcityMockPluginsTest : BaseMockPluginTest() {
  override fun getMockPluginBuildDirectory(): File = File("teamcity-mock-plugin/build/mocks")

  private fun testMockConfigs(plugin: TeamcityPlugin) {
    assertEquals(null, plugin.url)
    assertEquals("Google Compute Engine cloud integration", plugin.pluginName)
    assertEquals("@Version@", plugin.pluginVersion)
    assertEquals("cloud-google", plugin.pluginId)

    assertEquals("email@example.org", plugin.vendorEmail)
    assertEquals("http://www.jetbrains.com/", plugin.vendorUrl)
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

  @Test
  fun `plugin packed in zip`() {
    testMockPluginStructureAndConfiguration("mock-plugin-zip.zip")
  }

  @Test
  fun `plugin directory`() {
    testMockPluginStructureAndConfiguration("mock-plugin-dir")
  }

  private fun testMockPluginStructureAndConfiguration(pluginPath: String) {
    val pluginFile = getMockPluginFile(pluginPath)

    val pluginCreationResult = TeamcityPluginManager.createPlugin(pluginFile)
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
