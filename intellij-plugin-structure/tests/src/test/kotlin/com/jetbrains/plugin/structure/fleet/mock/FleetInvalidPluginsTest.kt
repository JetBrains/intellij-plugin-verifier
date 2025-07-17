package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.fleet.*
import com.jetbrains.plugin.structure.fleet.problems.InvalidSupportedProductsListProblem
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
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.readableName")) { it.copy(meta = it.meta?.copy(name = null)) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.readableName")) { it.copy(meta = it.meta?.copy(name = "")) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.readableName")) { it.copy(meta = it.meta?.copy(name = "\n")) }
    checkInvalidPlugin(
      TooLongPropertyValue(
        FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
        "compatibleShipVersionRange.readableName",
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
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.vendor")) { it.copy(meta = it.meta?.copy(vendor = null)) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.vendor")) { it.copy(meta = it.meta?.copy(vendor = "")) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.vendor")) { it.copy(meta = it.meta?.copy(vendor = "\n")) }
  }

  @Test
  fun `description is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.description")) { it.copy(meta = it.meta?.copy(description = null)) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.description")) { it.copy(meta = it.meta?.copy(description = "")) }
    checkInvalidPlugin(PropertyNotSpecified("compatibleShipVersionRange.description")) { it.copy(meta = it.meta?.copy(description = "\n")) }
  }

  @Test
  fun `do not validate compatibility range for ship`() {
    checkValidPlugin { it.copy(id = SHIP_PLUGIN_ID, compatibleShipVersionRange = null) }
  }

  @Test
  fun `invalid supported product`() {
    val supportedProducts = "LOL"
    checkInvalidPlugin(
      InvalidSupportedProductsListProblem("must contain only product codes from ${FleetProduct.values().map { it.productCode }}, got: [$supportedProducts]")
    ) { it.copy(meta = it.meta?.copy(supportedProducts = supportedProducts)) }
  }

  @Test
  fun `mix of legacy and unified versioning in supported product`() {
    checkInvalidPlugin(InvalidSupportedProductsListProblem("must contain either only legacy or only unified versioning products")) {
      it.copy(meta = it.meta?.copy(supportedProducts = "FL,AIR"))
    }
  }

  @Test
  fun `legacy versioning product`() {
    checkValidPlugin { it.copy(meta = it.meta?.copy(supportedProducts = "FL")) }
  }

  @Test
  fun `unified versioning product`() {
    checkValidPlugin { it.copy(meta = it.meta?.copy(supportedProducts = "AIR")) }
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
  fun `legacy compatibility range is valid`() {
    val supportedProducts = "FL"
    val legacyVersioningSpec = FleetDescriptorSpec.CompatibleShipVersion.LegacyVersioningSpec

    checkInvalidPlugin(InvalidSemverFormat(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      versionName = "compatibleShipVersionRange.from",
      version = "123"
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "123"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(InvalidSemverFormat(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      versionName = "compatibleShipVersionRange.to",
      version = "123"
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "123"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }

    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "major",
      versionName = "compatibleShipVersionRange.from",
      version = "7450.1.2",
      limit = legacyVersioningSpec.MAJOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "7450.1.2", to = "7450.1.2"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "major",
      versionName = "compatibleShipVersionRange.to",
      version = "7450.1.2",
      limit = legacyVersioningSpec.MAJOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "7450.1.2"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "minor",
      versionName = "compatibleShipVersionRange.from",
      version = "0.8192.2",
      limit = legacyVersioningSpec.MINOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "0.8192.2"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "minor",
      versionName = "compatibleShipVersionRange.to",
      version = "1.8192.2",
      limit = legacyVersioningSpec.MINOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "1.8192.2"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "patch",
      versionName = "compatibleShipVersionRange.from",
      version = "1.2.16384",
      limit = legacyVersioningSpec.PATCH_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.2.16384"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "patch",
      versionName = "compatibleShipVersionRange.to",
      version = "1.1000.16384",
      limit = legacyVersioningSpec.PATCH_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "1.1000.16384"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }

    checkInvalidPlugin(InvalidVersionRange(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      since = "1.1000.1",
      until = "1.1000.0"
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.1000.1", to = "1.1000.0"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }

    checkValidPlugin {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "7449.8191.16383", to = "7449.8191.16383"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
  }

  @Test
  fun `unified compatibility range is valid`() {
    val supportedProducts = "AIR"
    val legacyVersioningSpec = FleetDescriptorSpec.CompatibleShipVersion.UnifiedVersioningSpec

    checkInvalidPlugin(InvalidSemverFormat(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      versionName = "compatibleShipVersionRange.from",
      version = "123"
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "123"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(InvalidSemverFormat(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      versionName = "compatibleShipVersionRange.to",
      version = "123"
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(to = "123"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }

    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "major",
      versionName = "compatibleShipVersionRange.from",
      version = "1001.99999.9999",
      limit = legacyVersioningSpec.MAJOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1001.99999.9999", to = "1002.1.1"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "major",
      versionName = "compatibleShipVersionRange.to",
      version = "1001.99999.9999",
      limit = legacyVersioningSpec.MAJOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "123.1.1", to = "1001.99999.9999"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "minor",
      versionName = "compatibleShipVersionRange.from",
      version = "1.100001.9999",
      limit = legacyVersioningSpec.MINOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.100001.9999", to = "202.123.123"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "minor",
      versionName = "compatibleShipVersionRange.to",
      version = "1.100001.9999",
      limit = legacyVersioningSpec.MINOR_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.1.1", to = "1.100001.9999"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "patch",
      versionName = "compatibleShipVersionRange.from",
      version = "1.2.10001",
      limit = legacyVersioningSpec.PATCH_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.2.10001"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
    checkInvalidPlugin(SemverComponentLimitExceeded(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      componentName = "patch",
      versionName = "compatibleShipVersionRange.to",
      version = "1.2.10001",
      limit = legacyVersioningSpec.PATCH_PART_MAX_VALUE
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.1.1", to = "1.2.10001"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }

    checkInvalidPlugin(InvalidVersionRange(
      descriptorPath = FleetDescriptorSpec.DESCRIPTOR_FILE_NAME,
      since = "1.1000.1",
      until = "1.1000.0"
    )) {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "1.1000.1", to = "1.1000.0"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }

    checkValidPlugin {
      it.copy(
        compatibleShipVersionRange = it.compatibleShipVersionRange!!.copy(from = "252.8191.212", to = "253.8191.212"),
        meta = it.meta?.copy(supportedProducts = supportedProducts)
      )
    }
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