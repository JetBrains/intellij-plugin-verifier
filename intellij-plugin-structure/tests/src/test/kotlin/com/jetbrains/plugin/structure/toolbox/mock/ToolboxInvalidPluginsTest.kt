package com.jetbrains.plugin.structure.toolbox.mock

import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.toolbox.*
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class ToolboxInvalidPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<ToolboxPlugin, ToolboxPluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path) = ToolboxPluginManager.createManager(extractDirectory)

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertProblematicPlugin(Paths.get("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectToolboxPluginFile(incorrect.simpleName)))
  }

  @Test
  fun `name is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("meta.name")) { it.copy(meta = it.meta?.copy(name = null)) }
    checkInvalidPlugin(PropertyNotSpecified("meta.name")) { it.copy(meta = it.meta?.copy(name = "")) }
    checkInvalidPlugin(PropertyNotSpecified("meta.name")) { it.copy(meta = it.meta?.copy(name = "\n")) }
    checkInvalidPlugin(
      TooLongPropertyValue(
        ToolboxPluginManager.DESCRIPTOR_NAME,
        "meta.name",
        65,
        64
      )
    ) { it.copy(meta = it.meta?.copy(name = "a".repeat(65))) }
  }

  @Test
  fun `name contains unallowed symbols`() {
    for (i in 1..10) {
      val name = getRandomNotAllowedNameSymbols(i)
      checkInvalidPlugin(InvalidPluginName("extension.json", name)) {
        it.copy(meta = it.meta?.copy(name = name))
      }
    }
  }

  @Test
  fun `name contains only allowed symbols`() {
    for (i in 1..10) {
      val name = getRandomAllowedNameSymbols(i)
      checkValidPlugin {
        it.copy(meta = it.meta?.copy(name = name))
      }
    }
  }

  @Test
  fun `id is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("id")) { it.copy(id = null) }
    checkInvalidPlugin(PropertyNotSpecified("id")) { it.copy(id = "") }
    checkInvalidPlugin(PropertyNotSpecified("id")) { it.copy(id = "\n") }
    listOf("cat/../cat", "cat\\..\\cat").forEach { newId ->
      checkInvalidPlugin(InvalidPluginIDProblem(newId)) { it.copy(id = newId) }
    }
  }

  @Test
  fun `version is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("version")) { it.copy(version = null) }
    checkInvalidPlugin(PropertyNotSpecified("version")) { it.copy(version = "") }
    checkInvalidPlugin(PropertyNotSpecified("version")) { it.copy(version = "\n") }
  }

  @Test
  fun `vendor is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("meta.vendor")) { it.copy(meta = it.meta?.copy(vendor = null)) }
    checkInvalidPlugin(PropertyNotSpecified("meta.vendor")) { it.copy(meta = it.meta?.copy(vendor = "")) }
    checkInvalidPlugin(PropertyNotSpecified("meta.vendor")) { it.copy(meta = it.meta?.copy(vendor = "\n")) }
  }

  @Test
  fun `description is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("meta.description")) { it.copy(meta = it.meta?.copy(description = null)) }
    checkInvalidPlugin(PropertyNotSpecified("meta.description")) { it.copy(meta = it.meta?.copy(description = "")) }
    checkInvalidPlugin(PropertyNotSpecified("meta.description")) { it.copy(meta = it.meta?.copy(description = "\n")) }
    checkValidPlugin { it.copy(meta = it.meta?.copy(description = "herring herring herring")) }
  }

  @Test
  fun `url is not specified`() {
    checkValidPlugin { it.copy(meta = it.meta?.copy(url = null)) }
    checkInvalidPlugin(InvalidUrl("","meta.url")) { it.copy(meta = it.meta?.copy(url = "")) }
    checkInvalidPlugin(InvalidUrl("\n","meta.url")) { it.copy(meta = it.meta?.copy(url = "\n")) }
    checkInvalidPlugin(InvalidUrl("herring herring herring","meta.url")) { it.copy(meta = it.meta?.copy(url = "herring herring herring")) }
  }

  @Test
  fun `apiVersion is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("apiVersion")) { it.copy(apiVersion = null) }
    checkInvalidPlugin(PropertyNotSpecified("apiVersion")) { it.copy(apiVersion = "") }
    checkInvalidPlugin(PropertyNotSpecified("apiVersion")) { it.copy(apiVersion = "\n") }
  }

  @Test
  fun `apiVersion is valid`() {
    checkInvalidPlugin(InvalidSemverFormat(
      descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
      versionName = "apiVersion",
      version = "123"
    )) {
      it.copy(apiVersion = "123")
    }

    checkInvalidPlugin(listOf(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "major",
        versionName = "apiVersion",
        version = "7450.1.2",
        limit = ToolboxVersionRange.VERSION_MAJOR_PART_MAX_VALUE
      ),
    )) { it.copy(apiVersion = "7450.1.2") }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "minor",
        versionName = "apiVersion",
        version = "0.8192.2",
        limit = ToolboxVersionRange.VERSION_MINOR_PART_MAX_VALUE
      )
    ) {
      it.copy(apiVersion = "0.8192.2")
    }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "patch",
        versionName = "apiVersion",
        version = "1.2.1048577",
        limit = ToolboxVersionRange.VERSION_PATCH_PART_MAX_VALUE
      )
    ) {
      it.copy(apiVersion = "1.2.1048577")
    }

    checkValidPlugin { it.copy(apiVersion = "7449.8191.1048575") }
  }


  private fun checkInvalidPlugin(expectedProblem: PluginProblem, descriptorUpdater: (ToolboxPluginDescriptor) -> ToolboxPluginDescriptor) {
    val descriptor = descriptorUpdater(ToolboxPluginDescriptor.parse(getMockPluginJsonContent("extension")))
    Assert.assertEquals(listOf(expectedProblem), descriptor.validate())
  }

  private fun checkInvalidPlugin(expectedProblems: List<PluginProblem>, descriptorUpdater: (ToolboxPluginDescriptor) -> ToolboxPluginDescriptor) {
    val descriptor = descriptorUpdater(ToolboxPluginDescriptor.parse(getMockPluginJsonContent("extension")))
    Assert.assertEquals(expectedProblems, descriptor.validate())
  }

  private fun checkValidPlugin(descriptorUpdater: (ToolboxPluginDescriptor) -> ToolboxPluginDescriptor) {
    val descriptor = descriptorUpdater(ToolboxPluginDescriptor.parse(getMockPluginJsonContent("extension")))
    Assert.assertEquals(emptyList<PluginProblem>(), descriptor.validate())
  }
}