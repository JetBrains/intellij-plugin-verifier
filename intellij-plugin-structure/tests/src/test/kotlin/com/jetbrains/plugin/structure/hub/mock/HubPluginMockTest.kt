package com.jetbrains.plugin.structure.hub.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
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
    author = "Michael Jackson <mj@gmail.com>"
    homeUrl = "https://github.com/mariyadavydova/youtrack-cats-widget"
    description = "Funny cats and dogs for your Dashboard!"
    iconUrl = "images/cat_purr.png"
    dependencies = mapOf("Hub" to ">=2018.2")
    products = mapOf("Hub" to "^2018.2", "YouTrack" to "^2018.2")
  }

  private val iconTestContent = "<svg></svg>"

  @Test
  fun `hub plugin`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      dir("images") {
        file("cat_purr.png", iconTestContent)
      }
      file(HubPluginManager.DESCRIPTOR_NAME) {
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
    assertEquals("https://github.com/mariyadavydova/youtrack-cats-widget", plugin.url)
    assertEquals("Michael Jackson", plugin.vendor)
    assertEquals("mj@gmail.com", plugin.vendorEmail)
    assertEquals("Funny cats and dogs for your Dashboard!", plugin.description)
    assertEquals(iconTestContent, String(plugin.icons.single().content))
    assertEquals(mapOf("Hub" to ">=2018.2"), plugin.dependencies)
    assertEquals(mapOf("Hub" to "^2018.2", "YouTrack" to "^2018.2"), plugin.products)
    assertEquals(mockHubPluginManifestContent, plugin.manifestContent)
  }
}