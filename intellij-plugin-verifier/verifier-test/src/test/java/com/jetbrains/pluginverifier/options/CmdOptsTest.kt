package com.jetbrains.pluginverifier.options

import com.sampullara.cli.Args
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
}