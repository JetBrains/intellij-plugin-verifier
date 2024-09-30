package com.jetbrains.pluginverifier.tests

import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem
import com.jetbrains.pluginverifier.tests.bytecode.Dumps
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
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

}