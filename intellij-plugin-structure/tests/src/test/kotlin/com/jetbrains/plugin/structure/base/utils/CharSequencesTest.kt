package com.jetbrains.plugin.structure.base.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class CharSequencesTest {
  @Test
  fun `specific component is retrieved`() {
    val s: CharSequence = "/var/lib/example"
    assertEquals("var", s.componentAt(1, '/'))
    assertNull(s.componentAt(5, '/'))
    assertEquals(s, s.componentAt(0, '|'))
    assertNull(s.componentAt(1, '|'))
  }
}