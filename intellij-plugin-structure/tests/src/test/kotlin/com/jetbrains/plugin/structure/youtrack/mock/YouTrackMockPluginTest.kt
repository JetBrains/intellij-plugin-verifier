package com.jetbrains.plugin.structure.youtrack.mock

import com.jetbrains.plugin.structure.base.plugin.IconTheme
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.youtrack.YouTrackPlugin
import com.jetbrains.plugin.structure.youtrack.YouTrackPluginManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class YouTrackMockPluginTest(fileSystemType: FileSystemType) : BasePluginManagerTest<YouTrackPlugin, YouTrackPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): YouTrackPluginManager {
    return YouTrackPluginManager.createManager(extractDirectory)
  }

  @Test
  fun `should return youtrack plugin with all fields specified`() {
    val appFile = buildZipFile(temporaryFolder.newFile("app.zip")) {
      file("manifest.json", getMockPluginFileContent("manifest.json"))
      file("icon.svg", getMockPluginFileContent("icon.svg"))
    }
    val pluginCreationSuccess = createPluginSuccessfully(appFile)
    val plugin = pluginCreationSuccess.plugin

    assertTrue(pluginCreationSuccess.warnings.isEmpty())

    assertEquals("template-app", plugin.pluginId)
    assertEquals("Template App", plugin.pluginName)
    assertEquals("App description", plugin.description)
    assertEquals("1.0.0", plugin.pluginVersion)
    assertEquals("https://example.com", plugin.url)
    assertEquals("2022.2.0", plugin.sinceVersion)
    assertEquals("2024.2.0", plugin.untilVersion)
    assertEquals("Version 0.0.1: Feature 1\n\nVersion 1.0.0: Feature 2", plugin.changeNotes)

    assertEquals("JetBrains s.r.o.", plugin.vendor)
    assertEquals("support@jetbrains.com", plugin.vendorEmail)
    assertEquals("https://www.jetbrains.com/", plugin.vendorUrl)

    assertEquals(2, plugin.icons.size)
    assertTrue(plugin.icons.any { it.theme == IconTheme.DEFAULT })
    assertTrue(plugin.icons.any { it.theme == IconTheme.DARCULA })
    assertTrue(plugin.icons.all { it.content.isNotEmpty() })
  }
}