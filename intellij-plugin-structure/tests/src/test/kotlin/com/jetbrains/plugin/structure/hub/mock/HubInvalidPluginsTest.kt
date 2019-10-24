package com.jetbrains.plugin.structure.hub.mock

import com.jetbrains.plugin.structure.base.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.hub.HubPlugin
import com.jetbrains.plugin.structure.hub.HubPluginManager
import com.jetbrains.plugin.structure.hub.problems.HubDependenciesNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubProductsNotSpecified
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
      try {
        val creationFail = getFailedResult(pluginFile)
        val actualProblems = creationFail.errorsAndWarnings
        Assert.assertEquals(expectedProblems.toSet(), actualProblems.toSet())
      } finally {
        pluginFile.deleteLogged()
      }
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
    assertExpectedProblems(pluginFile, listOf(PluginDescriptorIsNotFound(HubPluginManager.DESCRIPTOR_NAME)))
  }

  @Test
  fun `manifest json not found in zip`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
    }
    assertExpectedProblems(pluginFile, listOf(PluginDescriptorIsNotFound(HubPluginManager.DESCRIPTOR_NAME)))
  }

  private fun checkInvalidPlugin(problem: PluginProblem, destructor: HubPluginJsonBuilder.() -> Unit) {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(HubPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectHubPluginBuilder
        builder.destructor()
        builder.asString()
      }
    }
    assertExpectedProblems(pluginFile, listOf(problem))
  }

  @Test
  fun `key is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("key")) { key = null }
    checkInvalidPlugin(PropertyNotSpecified("key")) { key = "" }
  }

  @Test
  fun `name is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("name")) { name = null }
    checkInvalidPlugin(PropertyNotSpecified("name")) { name = "" }
  }

  @Test
  fun `version is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("version")) { version = null }
    checkInvalidPlugin(PropertyNotSpecified("version")) { version = "" }
  }

  @Test
  fun `author is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("author")) { author = null }
    checkInvalidPlugin(PropertyNotSpecified("author")) { author = "" }
  }

  @Test
  fun `dependencies are not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("dependencies")) { dependencies = null }
    checkInvalidPlugin(HubDependenciesNotSpecified()) { dependencies = emptyMap() }
  }

  @Test
  fun `products are not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("products")) { products = null }
    checkInvalidPlugin(HubProductsNotSpecified()) { products = emptyMap() }
  }

  @Test
  fun `too many files in hub plugin`() {
    val tooManyNumber = 1001

    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(HubPluginManager.DESCRIPTOR_NAME) {
        perfectHubPluginBuilder.modify { }
      }
      (0 until tooManyNumber).forEach { i ->
        file("file_$i.txt", "$i")
      }
    }
    assertExpectedProblems(pluginFile, listOf(HubZipFileTooManyFilesError()))
  }

  @Test
  fun `too big hub zip file`() {
    val tooBigSize = 31 * 1024 * 1024

    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(HubPluginManager.DESCRIPTOR_NAME) {
        perfectHubPluginBuilder.modify { }
      }

      file("bigFile.bin", ByteArray(tooBigSize))
    }

    assertExpectedProblems(pluginFile, listOf(PluginFileSizeIsTooLarge(30 * 1024 * 1024)))
  }

}