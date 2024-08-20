package com.jetbrains.plugin.structure.intellij.version

import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion.Companion.parse
import org.junit.Assert.*
import org.junit.Test

class ProductReleaseVersionTest {
  @Test
  fun `release version is handled`() {
    with(parse("20201")) {
      assertEquals(2020, major)
      assertEquals(1, minor)
      assertEquals(20201, value)
    }

    with(parse("400")) {
      assertEquals(40, major)
      assertEquals(0, minor)
      assertEquals(400, value)
    }
  }

  @Test
  fun `illegal release version is handled`() {
    assertThrows(NumberFormatException::class.java) {
      parse("2020EAP")
    }.run {
      assertEquals("Release version [2020EAP] must be an integer", message)
    }
  }

  @Test
  fun `legacy release version`() {
    with(parse("8")) {
      assertTrue(isSingleDigit)
      assertEquals(8, major)
      assertEquals(0, minor)
      assertEquals(8, value)
    }
  }
}