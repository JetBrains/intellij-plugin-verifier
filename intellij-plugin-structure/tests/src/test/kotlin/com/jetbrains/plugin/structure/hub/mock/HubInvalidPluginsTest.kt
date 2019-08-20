package com.jetbrains.plugin.structure.hub.mock

import com.jetbrains.plugin.structure.base.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.hub.HubPlugin
import com.jetbrains.plugin.structure.hub.HubPluginManager
import com.jetbrains.plugin.structure.hub.problems.HubZipFileTooLargeError
import com.jetbrains.plugin.structure.hub.problems.HubZipFileTooManyFilesError
import com.jetbrains.plugin.structure.hub.problems.createIncorrectHubPluginFile
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HubInvalidPluginsTest {

  companion object {
    fun assertExpectedProblems(pluginFile: File, expectedProblems: List<PluginProblem>) {
      val creationFail = getFailedResult(pluginFile)
      val actualProblems = creationFail.errorsAndWarnings
      Assert.assertEquals(expectedProblems.toSet(), actualProblems.toSet())
    }

    private fun getFailedResult(pluginFile: File): PluginCreationFail<HubPlugin> {
      val pluginCreationResult = HubPluginManager.createManager().createPlugin(pluginFile)
      if (pluginCreationResult is PluginCreationSuccess) {
        Assert.fail("must have failed, but warnings: [${pluginCreationResult.warnings.joinToString()}]")
      }
      return pluginCreationResult as PluginCreationFail
    }
  }

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertExpectedProblems(File("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertExpectedProblems(incorrect, listOf(createIncorrectHubPluginFile(incorrect.name)))
  }

  @Test
  fun `manifest json not found in directory`() {
    val pluginFile = buildDirectory(temporaryFolder.newFolder("plugin")) {
    }
    assertExpectedProblems(pluginFile, listOf(PluginDescriptorIsNotFound("manifest.json")))
  }

  @Test
  fun `manifest json not found in zip`() {
    val pluginFile = buildZipFile(temporaryFolder.newFolder("plugin.zip")) {
    }
    assertExpectedProblems(pluginFile, listOf(PluginDescriptorIsNotFound("manifest.json")))
  }

  private fun checkPropertyNotSpecified(propertyName: String, destructor: HubPluginJsonBuilder.() -> Unit) {
    val pluginFile = buildZipFile(temporaryFolder.newFolder("plugin.zip")) {
      file("manifest.json") {
        val builder = perfectHubPluginBuilder
        builder.destructor()
        builder.asString()
      }
    }
    assertExpectedProblems(pluginFile, listOf(PropertyNotSpecified(propertyName)))
  }

  @Test
  fun `id is not specified`() {
    checkPropertyNotSpecified("key") { key = "" }
  }

  @Test
  fun `name is not specified`() {
    checkPropertyNotSpecified("name") { name = "" }
  }

  @Test
  fun `version is not specified`() {
    checkPropertyNotSpecified("version") { version = "" }
  }

  @Test
  fun `author is not specified`() {
    checkPropertyNotSpecified("author") { author = "" }
  }

  @Test
  fun `dependencies are not specified`() {
    checkPropertyNotSpecified("dependencies") { dependencies = emptyMap() }
  }

  @Test
  fun `products are not specified`() {
    checkPropertyNotSpecified("products") { products = emptyMap() }
  }

  @Test
  fun `too many files in hub plugin`() {
    val tooManyNumber = 1001

    val pluginFile = buildZipFile(temporaryFolder.newFolder("plugin.zip")) {
      file("manifest.json") {
        perfectHubPluginBuilder.modify {  }
      }
      (0 until tooManyNumber).forEach { i ->
        file("file_$i.txt", "$i")
      }
    }
    assertExpectedProblems(pluginFile, listOf(HubZipFileTooManyFilesError()))
  }

}