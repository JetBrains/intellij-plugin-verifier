package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MockThemePluginsTest  {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

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
  private fun testMockPluginStructureAndConfiguration(pluginFile: File) {
    val pluginCreationSuccess = InvalidPluginsTest.getSuccessResult(pluginFile)
    testPluginThemes(pluginCreationSuccess.plugin)
  }

  private fun testPluginThemes(plugin: IdePlugin) {
    assertEquals(
        setOf(IdeTheme("theme", true), IdeTheme("relativeTheme", false)),
        plugin.declaredThemes.toSet()
    )
  }
}
