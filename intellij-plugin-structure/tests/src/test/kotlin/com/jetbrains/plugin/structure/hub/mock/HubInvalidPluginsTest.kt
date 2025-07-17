package com.jetbrains.plugin.structure.hub.mock

import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.hub.HubPlugin
import com.jetbrains.plugin.structure.hub.HubPluginManager
import com.jetbrains.plugin.structure.hub.problems.HubDependenciesNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubProductsNotSpecified
import com.jetbrains.plugin.structure.hub.problems.createIncorrectHubPluginFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class HubInvalidPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<HubPlugin, HubPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): HubPluginManager =
    HubPluginManager.createManager(extractDirectory)

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertProblematicPlugin(Paths.get("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectHubPluginFile(incorrect.simpleName)))
  }

  @Test
  fun `manifest json not found in directory`() {
    val pluginFile = buildDirectory(temporaryFolder.newFolder("plugin")) {
    }
    assertProblematicPlugin(pluginFile, listOf(PluginDescriptorIsNotFound(HubPluginManager.DESCRIPTOR_NAME)))
  }

  @Test
  fun `manifest json not found in zip`() {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("plugin.zip")) {
    }
    assertProblematicPlugin(pluginFile, listOf(PluginDescriptorIsNotFound(HubPluginManager.DESCRIPTOR_NAME)))
  }

  private fun checkInvalidPlugin(problem: PluginProblem, destructor: HubPluginJsonBuilder.() -> Unit) {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("plugin.zip")) {
      file(HubPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectHubPluginBuilder
        builder.destructor()
        builder.asString()
      }
    }
    assertProblematicPlugin(pluginFile, listOf(problem))
  }

  @Test
  fun `key is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("key")) { key = null }
    checkInvalidPlugin(PropertyNotSpecified("key")) { key = "" }
  }

  @Test
  fun `invalid id`() {
    val invalidIds = listOf(
      "hello!world",
      "123@abc",
      "A#B%",
      "first second",
      "newline\nhere",
      "tab\there",
      "slash/backslash",
      "colon:semicolon;",
      "quote\"apostrophe'",
      "test~test"
    )

    invalidIds.forEach {
      checkInvalidPlugin(InvalidPluginIDProblem(it)) { key = it }
    }
  }

  @Test
  fun `name is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("name")) { name = null }
    checkInvalidPlugin(PropertyNotSpecified("name")) { name = "" }
    checkInvalidPlugin(
      TooLongPropertyValue(
        HubPluginManager.DESCRIPTOR_NAME,
        "name",
        65,
        64
      )
    ) { name = "a".repeat(65) }
  }

  @Test
  fun `name contains unallowed symbols`() {
    for (i in 1..10) {
      val pluginName = getRandomNotAllowedNameSymbols(i)
      checkInvalidPlugin(InvalidPluginName("manifest.json", pluginName)) {
        name = pluginName
      }
    }
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

    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("plugin.zip")) {
      file(HubPluginManager.DESCRIPTOR_NAME) {
        perfectHubPluginBuilder.modify { }
      }
      (0 until tooManyNumber).forEach { i ->
        file("file_$i.txt", "$i")
      }
    }
    assertProblematicPlugin(pluginFile, listOf(TooManyFiles(1000L)))
  }

  @Test
  fun `too big hub zip file`() {
    val tooBigSize = 31 * 1024 * 1024

    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("plugin.zip")) {
      file(HubPluginManager.DESCRIPTOR_NAME) {
        perfectHubPluginBuilder.modify { }
      }

      file("bigFile.bin", ByteArray(tooBigSize))
    }

    assertProblematicPlugin(pluginFile, listOf(PluginFileSizeIsTooLarge(30L * 1024 * 1024)))
  }

}