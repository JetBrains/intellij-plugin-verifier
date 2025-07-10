package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class MockThemePluginsTest(fileSystemType: FileSystemType) : IdePluginManagerTest(fileSystemType)  {

  @Test
  fun `jar file packed in zip`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar")) {
      dir("META-INF") {
        file("plugin.xml") {
          perfectXmlBuilder.modify {
            additionalContent = """
              <extensions defaultExtensionNs="com.intellij">
                  <!-- Add your extensions here -->
                  <themeProvider id="id0" path="/theme.theme.json"/>
                  <themeProvider id="id1" path="relativeTheme.theme.json"/>
              </extensions>
            """.trimIndent()
          }
        }
      }

      file(
        "theme.theme.json", """
        {
          "name": "theme",
          "dark": true,
          "author": "",
          "editorScheme": "/theme.xml",
          "ui": {}
        }
      """.trimIndent()
      )

      file(
        "relativeTheme.theme.json", """
        {
          "name": "relativeTheme",
          "dark": false,
          "author": "",
          "editorScheme": "/relativeTheme.xml",
          "ui": {
          }
        }
      """.trimIndent()
      )
    }

    testMockPluginStructureAndConfiguration(pluginFile)
  }

  @Suppress("SameParameterValue")
  private fun testMockPluginStructureAndConfiguration(pluginFile: Path) {
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    testPluginThemes(pluginCreationSuccess.plugin)
  }

  private fun testPluginThemes(plugin: IdePlugin) {
    assertEquals(
      setOf(IdeTheme("theme", true), IdeTheme("relativeTheme", false)),
      plugin.declaredThemes.toSet()
    )
  }

  @After
  fun tearDown() {
    close()
  }
}
