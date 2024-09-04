package com.jetbrains.plugin.structure.dotnet.mock

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.dotnet.DotNetDependency
import com.jetbrains.plugin.structure.dotnet.ReSharperPlugin
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path

class DotNetMockPluginTest(fileSystemType: FileSystemType) : BasePluginManagerTest<ReSharperPlugin, ReSharperPluginManager>(fileSystemType) {
  private fun testMockConfigs(plugin: ReSharperPlugin) {
    Assert.assertEquals("JetBrains.Mock", plugin.pluginId)
    Assert.assertEquals("${plugin.pluginId}.nuspec", plugin.nuspecFile.fileName)
    Assert.assertArrayEquals(getMockPluginXmlContent().toByteArray(), plugin.nuspecFile.content)
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

  override fun createManager(extractDirectory: Path): ReSharperPluginManager =
    ReSharperPluginManager.createManager(extractDirectory)

  @Test
  fun `nupkg plugin`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("jetbrains.mock.10.2.55.nupkg")) {
      file("JetBrains.Mock.nuspec", getMockPluginXmlContent())
      file("dependencies.json", thirdPartyLicenciesContent)
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  private fun getMockPluginXmlContent(): String {
    return this::class.java.getResource("/dotnet/JetBrains.Mock.nuspec").readText()
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: Path) {
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin

    testMockConfigs(plugin)
    testThirdPartyDependencies(plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

  private fun testThirdPartyDependencies(plugin: ReSharperPlugin) {
    val dependency = plugin.thirdPartyDependencies.first()
    Assert.assertEquals("TheCat", dependency.name)
    Assert.assertEquals("Custom license", dependency.license)
    Assert.assertEquals("https://github.com/rsdn/TheCat/TheCat/v1.1/TheCat", dependency.licenseUrl)
    Assert.assertEquals("1.2.0.547", dependency.version)
    Assert.assertEquals("https://github.com/TheCat/TheCat123", dependency.url)
  }

  private val thirdPartyLicenciesContent = """
    [
      {
        "name": "TheCat",
        "version": "1.2.0.547",
        "url": "https://github.com/TheCat/TheCat123",
        "license": "Custom license",
        "licenseUrl": "https://github.com/rsdn/TheCat/TheCat/v1.1/TheCat"
      }
    ]

  """.trimIndent()

}
