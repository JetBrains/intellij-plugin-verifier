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
    checkInvalidPlugin(PropertyNotSpecified("name")) { it.copy(meta = it.meta?.copy(name = null)) }
    checkInvalidPlugin(PropertyNotSpecified("name")) { it.copy(meta = it.meta?.copy(name = "")) }
    checkInvalidPlugin(PropertyNotSpecified("name")) { it.copy(meta = it.meta?.copy(name = "\n")) }
    checkInvalidPlugin(
      TooLongPropertyValue(
        ToolboxPluginManager.DESCRIPTOR_NAME,
        "name",
        65,
        64
      )
    ) { it.copy(meta = it.meta?.copy(name = "a".repeat(65))) }
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
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { it.copy(meta = it.meta?.copy(vendor = null)) }
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { it.copy(meta = it.meta?.copy(vendor = "")) }
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { it.copy(meta = it.meta?.copy(vendor = "\n")) }
  }

  @Test
  fun `description is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("description")) { it.copy(meta = it.meta?.copy(description = null)) }
    checkInvalidPlugin(PropertyNotSpecified("description")) { it.copy(meta = it.meta?.copy(description = "")) }
    checkInvalidPlugin(PropertyNotSpecified("description")) { it.copy(meta = it.meta?.copy(description = "\n")) }
  }

  @Test
  fun `compatibility range is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("compatibleVersionRange.from")) { it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = null)) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleVersionRange.from")) { it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "")) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleVersionRange.from")) { it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "\n")) }
  }

  @Test
  fun `compatibility range is valid`() {
    checkInvalidPlugin(InvalidSemverFormat(
      descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
      versionName = "compatibleVersionRange.from",
      version = "123"
    )) {
      it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "123"))
    }
    checkInvalidPlugin(InvalidSemverFormat(
      descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
      versionName = "compatibleVersionRange.to",
      version = "123"
    )) {
      it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(to = "123"))
    }

    checkInvalidPlugin(listOf(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "major",
        versionName = "compatibleVersionRange.from",
        version = "7450.1.2",
        limit = ToolboxVersionRange.VERSION_MAJOR_PART_MAX_VALUE
      ),
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "major",
        versionName = "compatibleVersionRange.to",
        version = "7450.1.2",
        limit = ToolboxVersionRange.VERSION_MAJOR_PART_MAX_VALUE
      )
    )) { it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "7450.1.2", to = "7450.1.2")) }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "major",
        versionName = "compatibleVersionRange.to",
        version = "7450.1.2",
        limit = ToolboxVersionRange.VERSION_MAJOR_PART_MAX_VALUE
      )
    ) {
      it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(to = "7450.1.2"))
    }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "minor",
        versionName = "compatibleVersionRange.from",
        version = "0.8192.2",
        limit = ToolboxVersionRange.VERSION_MINOR_PART_MAX_VALUE
      )
    ) {
      it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "0.8192.2"))
    }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "minor",
        versionName = "compatibleVersionRange.to",
        version = "1.8192.2",
        limit = ToolboxVersionRange.VERSION_MINOR_PART_MAX_VALUE
      )
    ) {
      it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(to = "1.8192.2"))
    }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "patch",
        versionName = "compatibleVersionRange.from",
        version = "1.2.1048577",
        limit = ToolboxVersionRange.VERSION_PATCH_PART_MAX_VALUE
      )
    ) {
      it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "1.2.1048577"))
    }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
        componentName = "patch",
        versionName = "compatibleVersionRange.to",
        version = "1.1000.1048577",
        limit = ToolboxVersionRange.VERSION_PATCH_PART_MAX_VALUE
      )
    ) {
      it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(to = "1.1000.1048577"))
    }

    checkInvalidPlugin(InvalidVersionRange(
      descriptorPath = ToolboxPluginManager.DESCRIPTOR_NAME,
      since = "1.1000.1",
      until = "1.1000.0"
    )) { it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "1.1000.1", to = "1.1000.0")) }

    checkValidPlugin { it.copy(compatibleVersionRange = it.compatibleVersionRange!!.copy(from = "7449.8191.1048575", to = "7449.8191.1048575")) }
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