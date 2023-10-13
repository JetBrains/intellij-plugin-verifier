package com.jetbrains.plugin.structure.teamcity.mock

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import com.jetbrains.plugin.structure.teamcity.TeamcityVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class TeamcityMockPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<TeamcityPlugin, TeamcityPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamcityPluginManager =
    TeamcityPluginManager.createManager(extractDirectory)

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

  @Test
  fun `plugin third party deps`() {
    val content = """
        [
      {
        "name": "TheCat",
        "version": "1.2.0.547",
        "url": "https://github.com/TheCat/TheCat123",
        "license": "Custom license",
        "licenseUrl": "https://github.com/rsdn/TheCat/TheCat/v1.1/TheCat"
      }
    ]
    """.trimIndent()
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file("teamcity-plugin.xml", getMockPluginXmlContent())
      file("dependencies.json", content)
    }
    val dependency = createPluginSuccessfully(pluginFile)
      .plugin.thirdPartyDependencies.first()
    assertEquals("TheCat", dependency.name)
    assertEquals("Custom license", dependency.license)
    assertEquals("https://github.com/rsdn/TheCat/TheCat/v1.1/TheCat", dependency.licenseUrl)
    assertEquals("1.2.0.547", dependency.version)
    assertEquals("https://github.com/TheCat/TheCat123", dependency.url)
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: Path) {
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin

    testMockConfigs(plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

}
