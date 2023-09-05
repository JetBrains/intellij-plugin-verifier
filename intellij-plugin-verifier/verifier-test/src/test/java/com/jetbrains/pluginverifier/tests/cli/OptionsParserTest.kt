package com.jetbrains.pluginverifier.tests.cli

import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.output.OutputFormat
import com.sampullara.cli.Args
import org.junit.Assert.*
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
  fun `verification output format is parsed with multiple unknown formats`() {
    val opts = CmdOpts(outputFormats = arrayOf("unexpected-broken-format", "-another-unexpected-broken-format"))
    val options = OptionsParser.parseOutputOptions(opts)
    with(options) {
      assertEquals(2, outputFormats.size)
      assertEquals(listOf(OutputFormat.PLAIN, OutputFormat.HTML), outputFormats)
    }
  }

  @Test
  fun `verification output format is parsed with unknown formats`() {
    val opts = CmdOpts(outputFormats = arrayOf("plain", "html", "unexpected-broken-format"))
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

  @Test
  fun `verification output format is empty and excludes plaintext`() {
    val opts = CmdOpts(outputFormats = arrayOf("-plain"))
    val options = OptionsParser.parseOutputOptions(opts)
    with(options) {
      assertEquals(1, outputFormats.size)
      assertEquals(listOf(OutputFormat.HTML), outputFormats)
    }
  }

  @Test
  fun `verification output format is specified, but empty string is provided to indicate no output formats`() {
    val args = arrayOf("-verification-reports-formats", "")
    val opts = CmdOpts()
    Args.parse(opts, args, false)

    val options = OptionsParser.parseOutputOptions(opts)
    with(options) {
      assertTrue(outputFormats.isEmpty())
    }
  }

  @Test
  fun `verification output format is specified, but no value is provided`() {
    val illegalArgumentException = assertThrows(IllegalArgumentException::class.java) {
      val args = arrayOf("-verification-reports-formats")
      val opts = CmdOpts()
      Args.parse(opts, args, false)
    }
    assertEquals("Must have a value for non-boolean argument verification-reports-formats", illegalArgumentException.message)
  }

  @Test
  fun `no verification output format is specified, so revert to defaults`() {
    val args = arrayOf("")

    val opts = CmdOpts()
    Args.parse(opts, args, false)

    val options = OptionsParser.parseOutputOptions(opts)
    with(options) {
      assertEquals(2, outputFormats.size)
      assertEquals(listOf(OutputFormat.PLAIN, OutputFormat.HTML), outputFormats)
    }
  }

}