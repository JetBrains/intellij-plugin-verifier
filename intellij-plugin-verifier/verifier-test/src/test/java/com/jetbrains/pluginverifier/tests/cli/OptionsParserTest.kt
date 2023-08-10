package com.jetbrains.pluginverifier.tests.cli

import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.output.OutputFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class OptionsParserTest {
  @Test
  fun `verification output format is parsed`() {
    val opts = CmdOpts(outputFormats = arrayOf("plain", "html"))
    val options = OptionsParser.parseOutputOptions(opts)
    with(options) {
      assertEquals(2, outputFormats.size)
      assertEquals(listOf(OutputFormat.PLAIN, OutputFormat.HTML), outputFormats)
    }
  }

  @Test
  fun `verification output format is parsed with unknown formats`() {
    val opts = CmdOpts(outputFormats = arrayOf("plain", "html", "chocolate"))
    val options = OptionsParser.parseOutputOptions(opts)
    with(options) {
      assertEquals(2, outputFormats.size)
      assertEquals(listOf(OutputFormat.PLAIN, OutputFormat.HTML), outputFormats)
    }
  }

  @Test
  fun `verification output format is empty, but defaults to HTML and plaintext`() {
    val opts = CmdOpts()
    val options = OptionsParser.parseOutputOptions(opts)
    with(options) {
      assertEquals(2, outputFormats.size)
      assertEquals(listOf(OutputFormat.PLAIN, OutputFormat.HTML), outputFormats)
    }
  }
}