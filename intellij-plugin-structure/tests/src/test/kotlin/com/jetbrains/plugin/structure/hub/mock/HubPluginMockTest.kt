package com.jetbrains.plugin.structure.hub.mock

import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.hub.HubPlugin
import com.jetbrains.plugin.structure.hub.HubPluginManager
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HubPluginMockTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val mockHubPluginManifestContent = perfectHubPluginBuilder.modify {
    key = "cat-widget"
    name = "Pets"
    version = "1.1.1"
    author = "Mariya Davydova <mrs.mariya.davydova@gmail.com>"
    homeUrl = "https://github.com/mariyadavydova/youtrack-cats-widget"
    description = "Funny cats and dogs for your Dashboard!"
    iconUrl = "images/cat_purr.png"
    dependencies = mapOf("Hub" to ">=2018.2")
    products = mapOf("Hub" to "^2018.2", "YouTrack" to "^2018.2")
  }

  @Test
  fun `hub plugin`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file("manifest.json") {
        mockHubPluginManifestContent
      }
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: File) {
    val pluginCreationResult = HubPluginManager.createManager().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      Assert.fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess

    testMockConfigs(pluginCreationSuccess.plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    Assert.assertTrue(problems.isEmpty())
  }

  private fun testMockConfigs(plugin: HubPlugin) {
    assertEquals("cat-widget", plugin.pluginId)
    assertEquals("Pets", plugin.pluginName)
    assertEquals("1.1.1", plugin.pluginVersion)
    assertEquals("Mariya Davydova <mrs.mariya.davydova@gmail.com>", plugin.author)
    assertEquals("https://github.com/mariyadavydova/youtrack-cats-widget", plugin.url)
    assertEquals("Mariya Davydova", plugin.vendor)
    assertEquals("mrs.mariya.davydova@gmail.com", plugin.vendorEmail)
    assertEquals("Funny cats and dogs for your Dashboard!", plugin.description)
    assertEquals("images/cat_purr.png", plugin.iconUrl)
    assertEquals(mapOf("Hub" to ">=2018.2"), plugin.dependencies)
    assertEquals(mapOf("Hub" to "^2018.2", "YouTrack" to "^2018.2"), plugin.products)
    assertEquals(mockHubPluginManifestContent, plugin.manifestContent)
  }
}