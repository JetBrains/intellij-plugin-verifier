package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.fleet.FleetPlugin
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class FleetPluginMockTest(fileSystemType: FileSystemType) : BasePluginManagerTest<FleetPlugin, FleetPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path) = FleetPluginManager.createManager(extractDirectory)

  @Test
  fun `parse base fields fleet plugin test`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension")
      }
    }
    testMockPluginStructureAndConfiguration(pluginFile).also {
      val plugin = it.plugin
      assertEquals("2.0.0-beta+exp.sha.5114f85", plugin.requires?.first()?.version?.max)
      assertEquals("1.0.0", plugin.requires?.first()?.version?.min)
    }
  }

  @Test
  fun `parse fields fleet plugin without version test`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension_wo_version")
      }
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  @Test
  fun `parse fleet icons test`() {
    val content = "<svg></svg>"
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension")
      }
      dir("META-INF") {
        file("pluginIcon.svg", content)
        file("pluginIcon_dark.svg", content)
      }
    }
    testMockPluginStructureAndConfiguration(pluginFile).also {
      val (f, s) = it.plugin.icons
      assertEquals(content, String(f.content))
      assertEquals(content, String(s.content))
    }
  }

  @Test
  fun `parse fleet incorrect icons test`() {
    val content = "<svg></svg>"
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension")
      }
      dir("META-INF") {
        file("pluginIcon.svg", content)
        file("pluginIcon_darkest.svg", content)
      }
    }
    testMockPluginStructureAndConfiguration(pluginFile).also {
      assertTrue("Invalid Fleet icons size", it.plugin.icons.size == 1)
      assertEquals(content, String(it.plugin.icons.single().content))
    }
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: Path): PluginCreationSuccess<FleetPlugin> {
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    testMockConfigs(pluginCreationSuccess.plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
    return pluginCreationSuccess
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    assertTrue(problems.isEmpty())
  }

  private fun testMockConfigs(plugin: FleetPlugin) {
    assertEquals("fleet.language.css", plugin.pluginId)
    assertEquals("CSS", plugin.pluginName)
    assertEquals("fleet.language.css.Css", plugin.entryPoint)
    assertEquals("JetBrains", plugin.vendor)
    assertEquals("CSS language support", plugin.description)
    assertEquals("1.0.0-SNAPSHOT", plugin.pluginVersion)
    assertTrue(plugin.requires?.isNotEmpty() == true)
    assertTrue(plugin.fullDescriptorJson.isNotEmpty())
    assertEquals("fleet.language.xml", plugin.requires?.first()?.id)
  }
}
