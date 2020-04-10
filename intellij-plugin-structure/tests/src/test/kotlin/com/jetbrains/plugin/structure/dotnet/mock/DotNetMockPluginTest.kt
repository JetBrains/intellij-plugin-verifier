package com.jetbrains.plugin.structure.dotnet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.dotnet.DotNetDependency
import com.jetbrains.plugin.structure.dotnet.ReSharperPlugin
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DotNetMockPluginTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private fun testMockConfigs(plugin: ReSharperPlugin) {
    Assert.assertEquals("JetBrains.Mock", plugin.pluginId)
    Assert.assertEquals("Some title", plugin.pluginName)
    Assert.assertEquals("10.2.55", plugin.pluginVersion)
    Assert.assertEquals(listOf("JetBrains"), plugin.authors)
    Assert.assertEquals("https://raw.githubusercontent.com/JetBrains/ExternalAnnotations/master/LICENSE.md", plugin.licenseUrl)
    Assert.assertEquals("https://github.com/JetBrains/ExternalAnnotations", plugin.url)
    Assert.assertEquals("ReSharper External Annotations for .NET framework and popular libraries.", plugin.description)
    Assert.assertEquals("Some summary", plugin.summary)
    Assert.assertEquals("ReSharper 8.2 compatibility", plugin.changeNotes)
    Assert.assertEquals("Copyright 2014 JetBrains", plugin.copyright)
    Assert.assertEquals(
      listOf(
          DotNetDependency("ReSharper", "[8.0, 8.3)"),
          DotNetDependency("Wave", null),
          DotNetDependency("Wave", "183.0.0")
      ),
      plugin.dependencies
    )
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    Assert.assertTrue(problems.isEmpty())
  }

  @Test
  fun `nupkg plugin`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("jetbrains.mock.10.2.55.nupkg")) {
      file("JetBrains.Mock.nuspec", getMockPluginXmlContent())
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  private fun getMockPluginXmlContent(): String {
    return this::class.java.getResource("/dotnet/JetBrains.Mock.nuspec").readText()
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: File) {
    val pluginCreationResult = ReSharperPluginManager.createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      Assert.fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess
    val plugin = pluginCreationSuccess.plugin

    testMockConfigs(plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

}
