package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.fleet.FleetPlugin
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import com.jetbrains.plugin.structure.fleet.FleetShipVersionRange
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path

class FleetPluginMockTest(fileSystemType: FileSystemType) : BasePluginManagerTest<FleetPlugin, FleetPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path) = FleetPluginManager.createManager(extractDirectory)

  @Test
  fun `parse base fields fleet plugin`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension")
      }
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  @Test
  fun `parse fleet icons`() {
    val content = "<svg></svg>"
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension")
      }
      file("pluginIcon.svg", content)
      file("pluginIcon_dark.svg", content)
    }
    testMockPluginStructureAndConfiguration(pluginFile).also {
      val (f, s) = it.plugin.icons
      assertEquals(content, String(f.content))
      assertEquals(content, String(s.content))

      val fileNames = it.plugin.files.map { file -> file.name }.toSet()
      assertTrue("File ${f.fileName} should not be in files list: $fileNames", f.fileName !in fileNames)
      assertTrue("File ${s.fileName} should not be in files list: $fileNames", s.fileName !in fileNames)
    }
  }

  @Test
  fun `parse fleet incorrect icons`() {
    val content = "<svg></svg>"
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension")
      }
      file("pluginIcon.svg", content)
      file("pluginIcon_darkest.svg", content)
    }
    testMockPluginStructureAndConfiguration(pluginFile).also {
      assertTrue("Invalid Fleet icons size", it.plugin.icons.size == 1)
      assertEquals(content, String(it.plugin.icons.single().content))
    }
  }

  @Test
  fun `parse licenses`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("extension")
      }
      file(FleetPluginManager.THIRD_PARTY_LIBRARIES_FILE_NAME) {
        """
          [ {
            "name" : "OkHttp",
            "url" : "https://square.github.io/okhttp/",
            "version" : "5.0.0-alpha.9",
            "license" : "Apache 2.0",
            "licenseUrl" : "https://www.apache.org/licenses/LICENSE-2.0"
          }, {
            "name" : "docker-java-core",
            "url" : "https://github.com/docker-java/docker-java",
            "version" : "3.2.6",
            "license" : "Apache 2.0",
            "licenseUrl" : "https://www.apache.org/licenses/LICENSE-2.0"
          } ]
        """.trimIndent()
      }
    }
    testMockPluginStructureAndConfiguration(pluginFile).also {
      assertEquals(
        listOf(
          ThirdPartyDependency(
            name = "OkHttp",
            url = "https://square.github.io/okhttp/",
            version = "5.0.0-alpha.9",
            license = "Apache 2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
          ),
          ThirdPartyDependency(
            name = "docker-java-core",
            url = "https://github.com/docker-java/docker-java",
            version = "3.2.6",
            license = "Apache 2.0",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
          ),
        ), it.plugin.thirdPartyDependencies
      )
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
    assertEquals("JetBrains", plugin.vendor)
    assertEquals("CSS language support", plugin.description)
    assertEquals("1.0.0-SNAPSHOT", plugin.pluginVersion)
    assertEquals(FleetShipVersionRange("1.1000.1", "1.1001.10"), plugin.compatibleShipVersionRange)
    assertEquals(true, plugin.frontendOnly)
  }
}
