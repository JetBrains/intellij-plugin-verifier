package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.fleet.FleetPlugin
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import fleet.bundles.PluginDescriptor
import com.jetbrains.plugin.structure.fleet.problems.createIncorrectFleetPluginFile
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
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
    checkInvalidPlugin(PropertyNotSpecified("name")) { copy(readableName = null )}
    checkInvalidPlugin(PropertyNotSpecified("name")) { copy(readableName = "" )}
    checkInvalidPlugin(PropertyNotSpecified("name")) { copy(readableName = "\n" )}
    checkInvalidPlugin(
      TooLongPropertyValue(
        FleetPluginManager.DESCRIPTOR_NAME,
        "name",
        65,
        64
      )
    ) { copy(readableName = "a".repeat(65) )}
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
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { copy(vendor = null )}
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { copy(vendor = "" )}
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { copy(vendor = "\n" )}
  }

  @Test
  fun `description is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("description")) { copy(description = null) }
    checkInvalidPlugin(PropertyNotSpecified("description")) { copy(description = "" )}
    checkInvalidPlugin(PropertyNotSpecified("description")) { copy(description = "\n" )}
  }

  private fun checkInvalidPlugin(problem: PluginProblem, descriptorUpdater: PluginDescriptor.() -> PluginDescriptor) {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectFleetPluginBuilder.descriptorUpdater()
        builder.encodeToString()
      }
    }
    assertProblematicPlugin(pluginFile, listOf(problem))
  }
}