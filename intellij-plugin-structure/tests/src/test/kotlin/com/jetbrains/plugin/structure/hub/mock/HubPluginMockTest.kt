package com.jetbrains.plugin.structure.hub.mock

import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.hub.HubPlugin
import com.jetbrains.plugin.structure.hub.HubPluginManager
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HubPluginMockTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `hub plugin`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file("manifest.json") {
        perfectHubPluginBuilder.modify {
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
    Assert.assertEquals("cat-widget", plugin.pluginId)
    Assert.assertEquals("Pets", plugin.pluginName)
    Assert.assertEquals("1.1.1", plugin.pluginVersion)
    Assert.assertEquals("Mariya Davydova <mrs.mariya.davydova@gmail.com>", plugin.author)
    Assert.assertEquals("https://github.com/mariyadavydova/youtrack-cats-widget", plugin.url)
    Assert.assertEquals("Mariya Davydova", plugin.vendor)
    Assert.assertEquals("mrs.mariya.davydova@gmail.com", plugin.vendorEmail)
    Assert.assertEquals("Funny cats and dogs for your Dashboard!", plugin.description)
    Assert.assertEquals("images/cat_purr.png", plugin.iconUrl)
    Assert.assertEquals(mapOf("Hub" to ">=2018.2"), plugin.dependencies)
    Assert.assertEquals(mapOf("Hub" to "^2018.2", "YouTrack" to "^2018.2"), plugin.products)
  }
}