package com.jetbrains.plugin.structure.dotnet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.dotnet.ReSharperPlugin
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import com.jetbrains.plugin.structure.dotnet.problems.IncorrectDotNetPluginFile
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class DotNetInvalidPluginTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertExpectedProblems(incorrect, listOf(IncorrectDotNetPluginFile(incorrect.name)))
  }

  @Test()
  fun `plugin file does not exist`() {
    val nonExistentFile = File("non-existent-file")
    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("Plugin file non-existent-file does not exist")
    TeamcityPluginManager.createManager().createPlugin(nonExistentFile)
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.nupkg")
    assertExpectedProblems(brokenZipArchive, listOf(UnableToExtractZip()))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val file = temporaryFolder.newFile("withoutDescriptor.nupkg")
    ZipOutputStream(FileOutputStream(file)).use { it.putNextEntry(ZipEntry("randomEntry.txt")) }
    assertExpectedProblems(file, listOf(PluginDescriptorIsNotFound("")))
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
    assertExpectedProblems(pluginFolder, expectedProblems)
  }

  private fun `test valid plugin xml`(pluginXmlContent: String) {
    val pluginFolder = getTempPluginArchive(pluginXmlContent)
    getSuccessResult(pluginFolder)
  }

  private fun assertExpectedProblems(pluginFile: File, expectedProblems: List<PluginProblem>) {
    val creationFail = getFailedResult(pluginFile)
    Assert.assertThat(creationFail.errorsAndWarnings, CoreMatchers.`is`(expectedProblems))
  }

  private fun getSuccessResult(pluginFile: File): PluginCreationSuccess<ReSharperPlugin> {
    val pluginCreationResult = ReSharperPluginManager.createPlugin(pluginFile)
    Assert.assertThat(pluginCreationResult, CoreMatchers.instanceOf(PluginCreationSuccess::class.java))
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun getFailedResult(pluginFile: File): PluginCreationFail<ReSharperPlugin> {
    val pluginCreationResult = ReSharperPluginManager.createPlugin(pluginFile)
    Assert.assertThat(pluginCreationResult, CoreMatchers.instanceOf(PluginCreationFail::class.java))
    return pluginCreationResult as PluginCreationFail
  }

  private fun getTempPluginArchive(pluginXmlContent: String): File {
    val pluginFile = temporaryFolder.newFile("archive.nupkg")
    ZipOutputStream(FileOutputStream(pluginFile)).use {
      it.putNextEntry(ZipEntry("Vendor.PluginName.nuspec"))
      it.write(pluginXmlContent.toByteArray())
      it.closeEntry()
    }
    return pluginFile
  }
}