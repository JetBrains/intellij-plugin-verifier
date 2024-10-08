package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem
import com.jetbrains.pluginverifier.tests.bytecode.Dumps
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.tests.mocks.bundledPlugin
import com.jetbrains.pluginverifier.tests.mocks.ideaPlugin
import com.jetbrains.pluginverifier.tests.mocks.withRootElement
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JsonPluginUsageTest : BaseBytecodeTest() {
  private val pluginSpec = IdeaPluginSpec("com.intellij.plugin", "JetBrains s.r.o.")

  @Test
  fun `plugin uses JSON classes but they are not available in the IDE`() {
    assertVerified {
      ide = buildIdeWithBundledPlugins {}
      plugin = prepareUsage(pluginSpec, "JsonPluginUsage", Dumps.JsonPluginUsage())
      kotlin = false
    }.run {
      with(compatibilityProblems) {
        assertEquals(1, size)
        assertContains(this, PackageNotFoundProblem::class)
      }
    }
  }

  @Test
  fun `plugin uses JSON classes, JSON plugin is declared, but without any classes`() {
    val jsonPluginId = "com.intellij.modules.json"
    val jsonPlugin = bundledPlugin {
      id = jsonPluginId
      descriptorContent = ideaPlugin(
        pluginId = jsonPluginId,
        pluginName = "JSON",
        vendor = "JetBrains s.r.o."
      ).withRootElement()
    }

    val targetIde = buildIdeWithBundledPlugins(listOf(jsonPlugin))
    assertEquals(2, targetIde.bundledPlugins.size)

    assertVerified {
      ide = targetIde
      plugin = prepareUsage(pluginSpec, "JsonPluginUsage", Dumps.JsonPluginUsage())
      kotlin = false
    }.run {
      with(compatibilityProblems) {
        assertContains(this, PackageNotFoundProblem::class)
      }
    }
  }

  @Test
  fun `plugin uses JSON classes, JSON plugin is declared and includes classes`() {
    val jsonPluginId = "com.intellij.modules.json"
    val jsonPlugin = bundledPlugin {
      id = jsonPluginId
      descriptorContent = ideaPlugin(
        pluginId = jsonPluginId,
        pluginName = "JSON",
        vendor = "JetBrains s.r.o."
      ).withRootElement()
      classContentBuilder = {
        dirs("com/intellij/json") {
          file("JsonParserDefinition.class", JsonParserDefinition())
        }
      }
    }

    val targetIde = buildIdeWithBundledPlugins(bundledCorePlugins = listOf(jsonPlugin))
    assertEquals(2, targetIde.bundledPlugins.size)

    assertVerified {
      ide = targetIde
      plugin = prepareUsage(pluginSpec, "JsonPluginUsage", Dumps.JsonPluginUsage())
      kotlin = false
    }.run {
      with(compatibilityProblems) {
        assertEquals(0, size)
      }
    }
  }

  fun findAfterIdeaBuildClassPath(): Path {
    val directory = Paths.get("after-idea", "build", "classes", "java", "main")
    return if (directory.exists()) {
      directory
    } else {
      Paths.get("verifier-test").resolve(directory).also { check(it.exists()) }
    }
  }

  private fun JsonParserDefinition(): ByteArray {
    return findAfterIdeaBuildClassPath()
      .resolve("com/intellij/json/JsonParserDefinition.class")
      .let { Files.readAllBytes(it) }
  }

}