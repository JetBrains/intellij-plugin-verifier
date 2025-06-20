package com.jetbrains.plugin.structure.base.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
  @Test
  fun `5GB is the IntelliJ plugin size limit`() {
    assertEquals(5_368_709_120L,Settings.INTELLIJ_PLUGIN_SIZE_LIMIT.getAsLong())
  }
}