package com.jetbrains.pluginverifier.tests

import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.modifiers.Modifiers.Modifier.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ModifiersTest {
  @Test
  fun `toString is generated`() {
    val modifiers = Modifiers.of(FINAL, PUBLIC, DEPRECATED)
    assertEquals("public, final, deprecated", modifiers.toString())
  }
}