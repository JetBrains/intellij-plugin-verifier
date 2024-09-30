package com.jetbrains.pluginverifier.tests

import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem
import com.jetbrains.pluginverifier.tests.bytecode.Dumps
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.tests.mocks.bundledPlugin
import com.jetbrains.pluginverifier.tests.mocks.ideaPlugin
import com.jetbrains.pluginverifier.tests.mocks.withRootElement
import org.junit.Assert.assertEquals
import org.junit.Test

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
}