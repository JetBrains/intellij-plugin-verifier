package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.fleet.*
import com.jetbrains.plugin.structure.fleet.problems.createIncorrectFleetPluginFile
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
  fun `do not validate compatibility range for ship`() {
    checkValidPlugin { it.copy(id = SHIP_PLUGIN_ID, compatibleShipVersionRange = null) }
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
    checkInvalidPlugin(InvalidSemverVersion(
      descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
      versionName = "compatibleShipVersionRange.from",
      version = "123"
    )) {
      it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "123"))
    }
    checkInvalidPlugin(InvalidSemverVersion(
      descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
      versionName = "compatibleShipVersionRange.to",
      version = "123"
    )) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "123")) }

    checkInvalidPlugin(FleetErroneousShipVersion("from", "major", "7450.1.2", limit = VERSION_MAJOR_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "7450.1.2", to = "7450.1.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("to", "major", "7450.1.2", limit = VERSION_MAJOR_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "7450.1.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("from", "minor", "0.8192.2", limit = VERSION_MINOR_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "0.8192.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("to", "minor", "1.8192.2", limit = VERSION_MINOR_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "1.8192.2")) }
    checkInvalidPlugin(FleetErroneousShipVersion("from", "patch", "1.2.16384", limit = VERSION_PATCH_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.2.16384")) }
    checkInvalidPlugin(FleetErroneousShipVersion("to", "patch", "1.1000.16384", limit = VERSION_PATCH_PART_MAX_VALUE)) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "1.1000.16384")) }

    checkInvalidPlugin(InvalidVersionRange(
      descriptorPath = FleetPluginManager.DESCRIPTOR_NAME,
      since = "1.1000.1",
      until = "1.1000.0"
    )) { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.1000.1", to = "1.1000.0")) }

    checkValidPlugin { it.copy(compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "7449.8191.16383", to = "7449.8191.16383")) }
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