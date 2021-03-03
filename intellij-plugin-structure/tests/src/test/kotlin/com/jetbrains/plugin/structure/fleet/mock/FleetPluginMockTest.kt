package com.jetbrains.plugin.structure.fleet.mock

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
    testMockPluginStructureAndConfiguration(pluginFile)
  }


  private fun testMockPluginStructureAndConfiguration(pluginFile: Path) {
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    testMockConfigs(pluginCreationSuccess.plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    assertTrue(problems.isEmpty())
  }

  private fun testMockConfigs(plugin: FleetPlugin) {
    assertEquals("fleet.language.css", plugin.pluginId)
    assertEquals("CSS", plugin.pluginName)
    assertEquals("fleet.language.css.Css", plugin.entryPoint)
    assertEquals("CSS language support", plugin.description)
    assertEquals("1.0.0-SNAPSHOT", plugin.pluginVersion)
    assertTrue(plugin.requires?.isNotEmpty() == true)
    assertEquals("fleet.language.xml", plugin.requires?.first()?.id)
    assertEquals("2.0.0-beta+exp.sha.5114f85", plugin.requires?.first()?.version?.max)
    assertEquals("1.0.0", plugin.requires?.first()?.version?.min)
  }
}
