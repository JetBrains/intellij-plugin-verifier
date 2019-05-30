package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MockThemePluginsTest : BaseMockPluginTest() {
  override fun getMockPluginBuildDirectory(): File = File("mock-theme-plugin").resolve("build").resolve("libs")

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun `jar file packed in zip`() {
    testMockPluginStructureAndConfiguration("mock-theme-plugin-1.0.jar")
  }

  private fun testMockPluginStructureAndConfiguration(pluginPath: String) {
    val pluginFile = getMockPluginFile(pluginPath)

    val extractDirectory = tempFolder.newFolder()
    val pluginCreationResult = IdePluginManager.createManager(extractDirectory).createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess
    val plugin = pluginCreationSuccess.plugin

    testPluginThemes(plugin)
  }

  private fun testPluginThemes(plugin: IdePlugin) {
    assertThat(plugin.declaredThemes, Matchers.containsInAnyOrder(IdeTheme("theme", true)))
    assertThat(plugin.declaredThemes, Matchers.hasSize(1))
  }
}
