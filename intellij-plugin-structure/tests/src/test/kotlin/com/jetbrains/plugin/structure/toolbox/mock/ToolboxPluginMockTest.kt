package com.jetbrains.plugin.structure.toolbox.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.toolbox.ToolboxPlugin
import com.jetbrains.plugin.structure.toolbox.ToolboxPluginManager
import com.jetbrains.plugin.structure.toolbox.ToolboxVersionRange
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path

class ToolboxPluginMockTest(fileSystemType: FileSystemType) :
    BasePluginManagerTest<ToolboxPlugin, ToolboxPluginManager>(fileSystemType) {

    override fun createManager(extractDirectory: Path) = ToolboxPluginManager.createManager(extractDirectory)

    @Test
    fun `parse base fields toolbox plugin`() {
        val pluginFile = buildZipFile(temporaryFolder.newFile("chunga.changa-1.0.0-SNAPSHOT.zip")) {
            extensionJson()
        }
        testMockPluginStructureAndConfiguration(pluginFile)
    }

    @Test
    fun `parse toolbox icons`() {
        val content = "<svg></svg>"
        val pluginFile = buildZipFile(temporaryFolder.newFile("chunga.changa-1.0.0-SNAPSHOT.zip")) {
            extensionJson()
            file("pluginIcon.svg", content)
            file("pluginIcon_dark.svg", content)
        }
        testMockPluginStructureAndConfiguration(pluginFile).also {
            val (f, s) = it.plugin.icons
            assertEquals(content, String(f.content))
            assertEquals(content, String(s.content))

            val fileNames = it.plugin.files.map { file -> file.fileName }.toSet()
            assertTrue("File ${f.fileName} should not be in files list: $fileNames", f.fileName !in fileNames)
            assertTrue("File ${s.fileName} should not be in files list: $fileNames", s.fileName !in fileNames)
        }
    }

    @Test
    fun `parse toolbox incorrect icons`() {
        val content = "<svg></svg>"
        val pluginFile = buildZipFile(temporaryFolder.newFile("chunga.changa-1.0.0-SNAPSHOT.zip")) {
            extensionJson()
            file("pluginIcon.svg", content)
            file("pluginIcon_darkest.svg", content)
        }
        testMockPluginStructureAndConfiguration(pluginFile).also {
            assertTrue("Invalid Toolbox icons size", it.plugin.icons.size == 1)
            assertEquals(content, String(it.plugin.icons.single().content))
        }
    }

    @Test
    fun `parse licenses`() {
        val pluginFile = buildZipFile(temporaryFolder.newFile("chunga.changa-1.0.0-SNAPSHOT.zip")) {
            extensionJson()
            file(ToolboxPluginManager.THIRD_PARTY_LIBRARIES_FILE_NAME) {
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

    private fun ContentBuilder.extensionJson() {
        file(ToolboxPluginManager.DESCRIPTOR_NAME) {
            getMockPluginJsonContent("extension")
        }
    }

    private fun testMockPluginStructureAndConfiguration(pluginFile: Path): PluginCreationSuccess<ToolboxPlugin> {
        val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
        testMockConfigs(pluginCreationSuccess.plugin)
        testMockWarnings(pluginCreationSuccess.warnings)
        return pluginCreationSuccess
    }

    private fun testMockWarnings(problems: List<PluginProblem>) {
        assertTrue(problems.isEmpty())
    }

    private fun testMockConfigs(plugin: ToolboxPlugin) {
        assertEquals("chunga.changa", plugin.pluginId)
        assertEquals("Chunga Changa", plugin.pluginName)
        assertEquals("JetBrains", plugin.vendor)
        assertEquals("Chunga Changa language support", plugin.description)
        assertEquals("0.1", plugin.pluginVersion)
        assertEquals(ToolboxVersionRange("1.1.1", "1.21.10"), plugin.compatibleVersionRange)
    }
}
