package com.jetbrains.plugin.structure.base.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
  @Test
  fun `1_5GB is the IntelliJ plugin size limit`() {
    assertEquals(1_610_612_736L,Settings.INTELLIJ_PLUGIN_SIZE_LIMIT.getAsLong())
  }
}