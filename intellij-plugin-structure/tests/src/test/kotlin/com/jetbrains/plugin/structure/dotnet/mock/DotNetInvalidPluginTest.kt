package com.jetbrains.plugin.structure.dotnet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.dotnet.ReSharperPlugin
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import com.jetbrains.plugin.structure.dotnet.problems.createIncorrectDotNetPluginFileProblem
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DotNetInvalidPluginTest(fileSystemType: FileSystemType) : BasePluginManagerTest<ReSharperPlugin, ReSharperPluginManager>(fileSystemType) {
  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  override fun createManager(extractDirectory: Path) =
    ReSharperPluginManager.createManager(extractDirectory)

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectDotNetPluginFileProblem(incorrect.simpleName)))
  }

  @Test
  fun `plugin file does not exist`() {
    val nonExistentFile = Paths.get("non-existent-file")
    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("Plugin file non-existent-file does not exist")
    createPluginSuccessfully(nonExistentFile)
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.nupkg")
    assertProblematicPlugin(brokenZipArchive, listOf(PluginDescriptorIsNotFound("*.nuspec")))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val file = temporaryFolder.newFile("withoutDescriptor.nupkg")
    ZipOutputStream(Files.newOutputStream(file)).use { it.putNextEntry(ZipEntry("randomEntry.txt")) }
    assertProblematicPlugin(file, listOf(PluginDescriptorIsNotFound("*.nuspec")))
  }

  @Test
  fun `completely invalid plugin descriptor`() {
    `test invalid plugin xml`(
      "abracadabra",
      listOf(UnexpectedDescriptorElements("unexpected element on line 1"))
    )
  }

  @Test
  fun `plugin id is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { id = "" },
      listOf(PropertyNotSpecified("id"))
    )
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { version = "" },
      listOf(PropertyNotSpecified("version"))
    )
  }

  @Test
  fun `plugin license is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { licenseUrl = "" },
      listOf(PropertyNotSpecified("licenseUrl"))
    )
  }

  @Test
  fun `plugin long name`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { title = "<title>${"a".repeat(65)}</title>" },
      listOf(TooLongPropertyValue("", "title", 65, 64))
    )
  }

  @Test
  fun `plugin authors is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { authors = "" },
      listOf(PropertyNotSpecified("authors"))
    )
  }

  @Test
  fun `plugin description is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { description = "" },
      listOf(PropertyNotSpecified("description"))
    )
  }

  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = getTempPluginArchive(pluginXmlContent)
    assertProblematicPlugin(pluginFolder, expectedProblems)
  }

  private fun getTempPluginArchive(pluginXmlContent: String): Path {
    val pluginFile = temporaryFolder.newFile("archive.nupkg")
    ZipOutputStream(Files.newOutputStream(pluginFile)).use {
      it.putNextEntry(ZipEntry("Vendor.PluginName.nuspec"))
      it.write(pluginXmlContent.toByteArray())
      it.closeEntry()
    }
    return pluginFile
  }
}