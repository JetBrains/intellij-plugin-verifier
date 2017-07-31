package com.jetbrains.structure.teamcity.mock

import com.jetbrains.structure.plugin.PluginCreationFail
import com.jetbrains.structure.plugin.PluginCreationSuccess
import com.jetbrains.structure.plugin.PluginProblem
import com.jetbrains.structure.teamcity.TeamcityPlugin
import com.jetbrains.structure.teamcity.TeamcityPluginManager
import com.jetbrains.structure.teamcity.TeamcityVersion
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TeamcityMockPluginsTest {

  private fun getMockPluginFile(mockName: String): File {
    //if run with gradle
    var pluginFile = File("teamcity-mock-plugin/build/mocks/", mockName)
    if (pluginFile.exists()) {
      return pluginFile
    }
    //if run with IDE test runner
    pluginFile = File("intellij-plugin-structure/tests/teamcity-mock-plugin/build/mocks", mockName)
    Assert.assertTrue("mock plugin " + mockName + " is not found in " + pluginFile.absolutePath, pluginFile.exists())
    return pluginFile
  }


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

    val pluginCreationResult = TeamcityPluginManager.createTeamcityPlugin(pluginFile)
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
