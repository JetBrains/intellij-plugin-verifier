package com.jetbrains.pluginverifier.options

import com.sampullara.cli.Args
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import junit.framework.TestCase.assertEquals
import org.junit.Test

class CmdOptsTest {
  @Test
  fun `wrong value in suppress-internal-api-usages is handled`() {
    val args = arrayOf("-suppress-internal-api-usages", "unsupported")
    val opts = CmdOpts()
    Args.parse(opts, args, false)
    assertEquals("unsupported", opts.suppressInternalApiUsageWarnings)
  }

  @Test
  fun `ignore os arch flag is disabled by default and can be enabled`() {
    val defaultOpts = CmdOpts()
    assertFalse(defaultOpts.ignoreOsArch)

    val opts = CmdOpts()
    Args.parse(opts, arrayOf("-ignore-os-arch"), false)
    assertTrue(opts.ignoreOsArch)
  }
}