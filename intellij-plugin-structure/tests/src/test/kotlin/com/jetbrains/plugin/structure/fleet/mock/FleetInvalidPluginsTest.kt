package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.fleet.FleetPlugin
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import com.jetbrains.plugin.structure.fleet.problems.createIncorrectFleetPluginFile
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import fleet.bundles.BundleSpec
import fleet.bundles.KnownMeta
import fleet.bundles.encodeToString
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
    checkInvalidPlugin(PropertyNotSpecified("name")) { withMeta(KnownMeta.ReadableName, null) }
    checkInvalidPlugin(PropertyNotSpecified("name")) { withMeta(KnownMeta.ReadableName, "") }
    checkInvalidPlugin(PropertyNotSpecified("name")) { withMeta(KnownMeta.ReadableName, "\n") }
    checkInvalidPlugin(
      TooLongPropertyValue(
        FleetPluginManager.DESCRIPTOR_NAME,
        "name",
        65,
        64
      )
    ) { withMeta(KnownMeta.ReadableName, "a".repeat(65)) }
  }

/*
  @Test
  fun `id is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("id")) { copy(id = null )}
    checkInvalidPlugin(PropertyNotSpecified("id")) { copy(id = "" )}
    checkInvalidPlugin(PropertyNotSpecified("id")) { copy(id = "\n" )}
    listOf("cat/../cat", "cat\\..\\cat").forEach {
      checkInvalidPlugin(InvalidPluginIDProblem(it)) { copy(id = BundleName(it)) }
    }
  }
*/

  @Test
  fun `vendor is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { withMeta(KnownMeta.Vendor, null) }
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { withMeta(KnownMeta.Vendor, "") }
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { withMeta(KnownMeta.Vendor, "\n") }
  }

  @Test
  fun `description is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("description")) { withMeta(KnownMeta.Description, null) }
    checkInvalidPlugin(PropertyNotSpecified("description")) { withMeta(KnownMeta.Description, "") }
    checkInvalidPlugin(PropertyNotSpecified("description")) { withMeta(KnownMeta.Description, "\n") }
  }

  private fun checkInvalidPlugin(problem: PluginProblem, descriptorUpdater: BundleSpec.() -> BundleSpec) {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectFleetPluginBuilder.descriptorUpdater()
        builder.encodeToString()
      }
    }
    assertProblematicPlugin(pluginFile, listOf(problem))
  }

  private fun BundleSpec.withMeta(key: String, value: String?): BundleSpec =
    copy(
      bundle = bundle.copy(
        meta = if (value == null) {
          bundle.meta.filterKeys { it != key }
        } else {
          bundle.meta + mapOf(key to value)
        }
      )
    )
}