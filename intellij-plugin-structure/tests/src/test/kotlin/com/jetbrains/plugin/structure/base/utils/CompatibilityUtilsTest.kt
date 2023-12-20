package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Test
import org.junit.Assert.assertEquals

class CompatibilityUtilsTest {
  val maxVersionAsLongNumber = 999999990000

  @Test
  fun getMaxVersionAsLong() {
    assertEquals(maxVersionAsLongNumber, IdeVersion.maxVersion.asLong())
  }
}