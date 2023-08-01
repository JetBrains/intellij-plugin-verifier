package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginCreationResultResolverTest {
  @Test
  fun `unregistered plugin problem is detected`() {
    val plugin = IdePluginImpl()

    val adapter = IntelliJPluginCreationResultResolver()
    val pluginCreationResult = adapter.resolve(plugin, listOf(MockPluginProblem()))
    assertTrue(pluginCreationResult is PluginCreationFail)
    val failure = pluginCreationResult as PluginCreationFail
    assertEquals(1, failure.errorsAndWarnings.size)

    val pluginProblem = failure.errorsAndWarnings[0]
    assertTrue(pluginProblem is BlocklistedPluginError)
    val unsupportedPluginError = pluginProblem as BlocklistedPluginError

    assertTrue(unsupportedPluginError.cause is MockPluginProblem)
  }
}