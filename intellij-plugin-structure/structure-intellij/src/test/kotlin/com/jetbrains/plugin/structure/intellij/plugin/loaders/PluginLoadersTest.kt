package com.jetbrains.plugin.structure.intellij.plugin.loaders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginLoadersTest {
  @Test
  fun `load xml marks document without xinclude`() {
    val loadedXml = "<idea-plugin />".byteInputStream().loadXml()

    assertFalse(loadedXml.mayHaveXIncludes)
  }

  @Test
  fun `load xml marks document with xinclude namespace`() {
    val loadedXml = """
      <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
        <xi:include href="other.xml" />
      </idea-plugin>
    """.trimIndent().byteInputStream().loadXml()

    assertTrue(loadedXml.mayHaveXIncludes)
  }
}
