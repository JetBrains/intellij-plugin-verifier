package com.jetbrains.pluginverifier.misc

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter

private const val CHUNK_SIZE = 8192

class HtmlBuilderTextTest {

  @Test
  fun `short texts are printed same as strings`() {
    listOf(
      "",
      "\n",
      "\n\n",
      "plain",
      "single line <&>\"\n",
      "multiple\nlines <&>\n",
      "multiple\nlines without trailing line break",
      "emoji \uD83D\uDE00 and \u00A0 nbsp\n",
    ).forEach { assertPrintedSameAsString(it) }
  }

  @Test
  fun `text longer than a chunk is printed same as string`() {
    // Escaped character at the end of the first chunk, then at the start of the second one
    assertPrintedSameAsString("a".repeat(CHUNK_SIZE - 1) + "<&" + "b".repeat(10))
    // Surrogate pair split between chunks
    assertPrintedSameAsString("a".repeat(CHUNK_SIZE - 1) + "\uD83D\uDE00" + "b".repeat(10) + "\n")
    // Trailing line break as the last character of a full chunk
    assertPrintedSameAsString("a".repeat(CHUNK_SIZE - 1) + "\n")
    // Line breaks at the end of a chunk and at the start of the next one
    assertPrintedSameAsString("a".repeat(CHUNK_SIZE - 1) + "\n\n")
    // Single line spanning several chunks
    assertPrintedSameAsString("x<".repeat(3 * CHUNK_SIZE) + "\n")
  }

  @Test
  fun `text read by single characters is printed same as string`() {
    listOf(
      "",
      "\n",
      "\n\n",
      "single line <&>\n",
      "multiple\nlines <&>\n",
      "emoji \uD83D\uDE00\n",
    ).forEach { assertPrintedSameAsString(it) { input -> SingleCharReader(StringReader(input)) } }
  }

  private fun assertPrintedSameAsString(input: String, readerFactory: (String) -> Reader = ::StringReader) {
    val expected = print { +input.removeSuffix("\n") }
    val actual = print { text(readerFactory(input), trimTrailingNewline = true) }
    assertEquals(expected, actual)

    val expectedUntrimmed = print { +input }
    val actualUntrimmed = print { text(readerFactory(input)) }
    assertEquals(expectedUntrimmed, actualUntrimmed)
  }

  private fun print(content: HtmlBuilder.() -> Unit): String {
    val writer = StringWriter()
    HtmlBuilder(writer).apply {
      div {
        pre {
          content()
        }
      }
    }
    return writer.toString()
  }

  private class SingleCharReader(private val delegate: Reader) : Reader() {
    override fun read(cbuf: CharArray, off: Int, len: Int): Int = delegate.read(cbuf, off, minOf(len, 1))

    override fun close() = delegate.close()
  }
}
