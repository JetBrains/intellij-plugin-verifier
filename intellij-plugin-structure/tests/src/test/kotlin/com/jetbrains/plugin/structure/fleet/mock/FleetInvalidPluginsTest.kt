package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidPluginIDProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.fleet.FleetPlugin
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import com.jetbrains.plugin.structure.fleet.problems.createIncorrectFleetPluginFile
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
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
    checkInvalidPlugin(PropertyNotSpecified("name")) { name = null }
    checkInvalidPlugin(PropertyNotSpecified("name")) { name = "" }
    checkInvalidPlugin(PropertyNotSpecified("name")) { name = "\n" }
    checkInvalidPlugin(
      TooLongPropertyValue(
        FleetPluginManager.DESCRIPTOR_NAME,
        "name",
        65,
        64
      )
    ) { name = "a".repeat(65) }
  }

  @Test
  fun `id is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("id")) { id = null }
    checkInvalidPlugin(PropertyNotSpecified("id")) { id = "" }
    checkInvalidPlugin(PropertyNotSpecified("id")) { id = "\n" }
    listOf("cat/../cat", "cat\\..\\cat").forEach {
      checkInvalidPlugin(InvalidPluginIDProblem(it)) { id = it }
    }
  }

  @Test
  fun `vendor is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { vendor = null }
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { vendor = "" }
    checkInvalidPlugin(PropertyNotSpecified("vendor")) { vendor = "\n" }
  }

  @Test
  fun `description is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("description")) { description = null }
    checkInvalidPlugin(PropertyNotSpecified("description")) { description = "" }
    checkInvalidPlugin(PropertyNotSpecified("description")) { description = "\n" }
  }


  @Test
  fun `entryPoint is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("entryPoint")) { entryPoint = null }
    checkInvalidPlugin(PropertyNotSpecified("entryPoint")) { entryPoint = "" }
    checkInvalidPlugin(PropertyNotSpecified("entryPoint")) { entryPoint = "\n" }
  }


  private fun checkInvalidPlugin(problem: PluginProblem, descriptor: FleetTestDescriptor.() -> Unit) {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("fleet.language.css-1.0.0-SNAPSHOT.zip")) {
      file(FleetPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectFleetPluginBuilder
        builder.descriptor()
        builder.asString()
      }
    }
    assertProblematicPlugin(pluginFile, listOf(problem))
  }
}