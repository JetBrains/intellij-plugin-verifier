package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidPluginIDProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.fleet.*
import com.jetbrains.plugin.structure.fleet.problems.createIncorrectFleetPluginFile
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class FleetInvalidPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<FleetPlugin, FleetPluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path) = FleetPluginManager.createManager(extractDirectory)

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertProblematicPlugin(Paths.get("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectFleetPluginFile(incorrect.simpleName)))
  }

  @Test
  fun `name is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("name")) { it.copy(meta = it.meta?.copy(name = null)) }
    checkInvalidPlugin(PropertyNotSpecified("name")) { it.copy(meta = it.meta?.copy(name = "")) }
    checkInvalidPlugin(PropertyNotSpecified("name")) { it.copy(meta = it.meta?.copy(name = "\n")) }
    checkInvalidPlugin(
      TooLongPropertyValue(
        FleetPluginManager.DESCRIPTOR_NAME,
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
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange")) { it.copy(compatibleShipVersionRange = null) }

    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.from")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = null)) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.from")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "")) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.from")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "\n")) }

    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.to")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = null)) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.to")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "")) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.to")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "\n")) }
  }

  @Test
  fun `compatibility range is valid`() {
    checkInvalidPlugin(FleetInvalidShipVersion("from", "123")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "123")) }
    checkInvalidPlugin(FleetInvalidShipVersion("to", "123")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "123")) }

    checkInvalidPlugin(FleetErroneousShipVersion("from", "major", "2097152.1.2", limit = VERSION_MAJOR_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "2097152.1.2", to = "2097152.1.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("to", "major", "2097152.1.2", limit = VERSION_MAJOR_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "2097152.1.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("from", "minor", "0.4194304.2", limit = VERSION_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "0.4194304.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("to", "minor", "1.4194304.2", limit = VERSION_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "1.4194304.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("from", "patch", "1.2.4194304", limit = VERSION_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.2.4194304")) }
    checkInvalidPlugin(FleetErroneousShipVersion("to", "patch", "1.1000.4194304", limit = VERSION_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "1.1000.4194304")) }

    checkInvalidPlugin(FleetInvalidShipVersionRange(from = "1.1000.1", to = "1.1000.0")) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.1000.1", to = "1.1000.0")) }

    checkValidPlugin { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "2097151.4194303.4194303", to = "2097151.4194303.4194303")) }
  }


  private fun checkInvalidPlugin(expectedProblem: PluginProblem, descriptorUpdater: (FleetPluginDescriptor) -> FleetPluginDescriptor) {
    val descriptor = descriptorUpdater(FleetPluginDescriptor.parse(getMockPluginJsonContent("extension")))
    Assert.assertEquals(listOf(expectedProblem), descriptor.validate())
  }

  private fun checkValidPlugin(descriptorUpdater: (FleetPluginDescriptor) -> FleetPluginDescriptor) {
    val descriptor = descriptorUpdater(FleetPluginDescriptor.parse(getMockPluginJsonContent("extension")))
    Assert.assertEquals(emptyList<PluginProblem>(), descriptor.validate())
  }
}