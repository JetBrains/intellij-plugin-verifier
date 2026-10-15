/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

import com.intellij.platform.pluginSystem.parser.impl.LoadedXIncludeReference
import com.intellij.platform.pluginSystem.parser.impl.XIncludeLoader
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RecordingXIncludeLoaderTest {
  @Test
  fun `returns the delegate result unchanged and records every occurrence`() {
    val bytes = "<idea-plugin/>".toByteArray()
    val loaded = LoadedXIncludeReference(bytes, "plugin.jar!/META-INF/included.xml")
    val recorder = RecordingXIncludeLoader(loader { loaded })

    assertSame(loaded, recorder.loadXIncludeReference("META-INF/included.xml"))
    assertSame(loaded, recorder.loadXIncludeReference("META-INF/included.xml"))

    val attempts = recorder.attempts.filterIsInstance<XIncludeLoadAttempt.Loaded>()
    assertEquals(2, attempts.size)
    attempts.forEach {
      assertEquals("META-INF/included.xml", it.path)
      assertArrayEquals(bytes, it.bytes)
      assertEquals("plugin.jar!/META-INF/included.xml", it.diagnosticLocation)
    }
  }

  @Test
  fun `records a missing include without changing the null result`() {
    val recorder = RecordingXIncludeLoader(loader { null })

    assertNull(recorder.loadXIncludeReference("META-INF/missing.xml"))

    assertEquals(listOf(XIncludeLoadAttempt.Missing("META-INF/missing.xml")), recorder.attempts)
  }

  @Test
  fun `records and rethrows delegate failures`() {
    val failure = IllegalStateException("broken archive")
    val recorder = RecordingXIncludeLoader(loader { throw failure })

    try {
      recorder.loadXIncludeReference("META-INF/broken.xml")
      throw AssertionError("Expected the delegate failure")
    } catch (actual: IllegalStateException) {
      assertSame(failure, actual)
    }

    assertEquals(
      listOf(
        XIncludeLoadAttempt.Failed(
          "META-INF/broken.xml",
          IllegalStateException::class.java.name,
          "broken archive"
        )
      ),
      recorder.attempts
    )
  }

  private fun loader(load: (String) -> LoadedXIncludeReference?): XIncludeLoader = object : XIncludeLoader {
    override fun loadXIncludeReference(path: String): LoadedXIncludeReference? = load(path)
  }
}
