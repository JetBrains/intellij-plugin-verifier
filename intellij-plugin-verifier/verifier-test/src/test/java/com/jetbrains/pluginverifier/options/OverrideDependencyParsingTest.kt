package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.pluginverifier.tests.mocks.createPluginArchiveManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class OverrideDependencyParsingTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `returns null when no overrides are specified`() {
    val opts = CmdOpts()
    val archiveManager = temporaryFolder.createPluginArchiveManager()

    assertNull(OptionsParser.parseOverrideDependencyRepository(opts, archiveManager))
  }

  @Test
  fun `loads plugin from local jar and registers it by plugin ID`() {
    val pluginId = "my.override.plugin"
    val pluginFile = buildPluginJar(pluginId)

    val opts = CmdOpts().apply {
      overrideDependencies = arrayOf("$pluginId=$pluginFile")
    }
    val archiveManager = temporaryFolder.createPluginArchiveManager()

    val repository = OptionsParser.parseOverrideDependencyRepository(opts, archiveManager)

    assertNotNull(repository)
    val found = repository!!.getAllVersionsOfPlugin(pluginId)
    assertEquals(1, found.size)
    assertEquals(pluginId, found.single().pluginId)
  }

  @Test
  fun `rejects override when loaded plugin ID does not match the declared key`() {
    val pluginFile = buildPluginJar("actual.plugin.id")

    val opts = CmdOpts().apply {
      overrideDependencies = arrayOf("wrong.plugin.id=$pluginFile")
    }
    val archiveManager = temporaryFolder.createPluginArchiveManager()

    assertThrows(IllegalArgumentException::class.java) {
      OptionsParser.parseOverrideDependencyRepository(opts, archiveManager)
    }
  }

  @Test
  fun `rejects override when path does not exist`() {
    val opts = CmdOpts().apply {
      overrideDependencies = arrayOf("my.override.plugin=/nonexistent/path/plugin.jar")
    }
    val archiveManager = temporaryFolder.createPluginArchiveManager()

    assertThrows(IllegalArgumentException::class.java) {
      OptionsParser.parseOverrideDependencyRepository(opts, archiveManager)
    }
  }

  @Test
  fun `rejects override entry without equals separator`() {
    val opts = CmdOpts().apply {
      overrideDependencies = arrayOf("no-equals-sign")
    }
    val archiveManager = temporaryFolder.createPluginArchiveManager()

    assertThrows(IllegalArgumentException::class.java) {
      OptionsParser.parseOverrideDependencyRepository(opts, archiveManager)
    }
  }

  @Test
  fun `loads multiple overrides and each is retrievable by its own plugin ID`() {
    val idA = "acme.plugin.alpha"
    val idB = "acme.plugin.beta"
    val fileA = buildPluginJar(idA)
    val fileB = buildPluginJar(idB)

    val opts = CmdOpts().apply {
      overrideDependencies = arrayOf("$idA=$fileA", "$idB=$fileB")
    }
    val archiveManager = temporaryFolder.createPluginArchiveManager()

    val repository = OptionsParser.parseOverrideDependencyRepository(opts, archiveManager)!!

    assertEquals(idA, repository.getAllVersionsOfPlugin(idA).single().pluginId)
    assertEquals(idB, repository.getAllVersionsOfPlugin(idB).single().pluginId)
    assertEquals(0, repository.getAllVersionsOfPlugin("unrelated.plugin").size)
  }

  private fun buildPluginJar(pluginId: String): Path {
    val jarFile = temporaryFolder.newFile("$pluginId.jar").toPath()
    return buildZipFile(jarFile) {
      dir("META-INF") {
        file("plugin.xml") {
          """
          <idea-plugin>
            <id>$pluginId</id>
            <name>Override Test</name>
            <version>1.0.0</version>
            <vendor email="test@test.com" url="https://test.com">Test Vendor</vendor>
            <description>A sufficiently long description for the override test plugin fixture.</description>
            <change-notes>Initial release of the override test plugin fixture.</change-notes>
            <idea-version since-build="131.1"/>
          </idea-plugin>
          """.trimIndent()
        }
      }
    }
  }
}
