package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert
import org.junit.Test


class IdeVersionAsLongTest {
  @Test
  fun `ide version as long`() {
    val data = mapOf<String, Long>(
      "40.2250" to 40022500000,
      "40.2250.1" to 40022500001,
      "201.0" to 201000000000,
      "*" to 88999990000,
      "88.*" to 88999990000,
      "88.0.*" to 88000009999,
      "88.0.0.*" to 88000000000,
      "0" to 0,
      "203.4203.*" to 203042039999,
      "223.999999" to 223999990000,
      "999.100000000" to 999999990000
    )

    data.forEach { (k, v) ->
      val version = IdeVersion.createIdeVersion(k)

      Assert.assertEquals("Ide version (as long) is not as expected", v, version.asLong())
    }
  }
}