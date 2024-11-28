package com.jetbrains.pluginverifier.options

import org.junit.Assert.assertThrows
import org.junit.Test

class OptionsParserTest {
  @Test
  fun `incorrect internal API verification mode is marked as an error`() {
    val opts = CmdOpts().apply {
      suppressInternalApiUsageWarnings = "invalid"
    }
    assertThrows(IllegalArgumentException::class.java) {
      OptionsParser.parseInternalApiVerificationMode(opts)
    }
  }
}