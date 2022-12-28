package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.dotnet.version.ReSharperVersion
import org.junit.Assert
import org.junit.Test

class ReSharperVersionAsLongTest {
  @Test
  fun `resharper version as long`() {
    val data = mapOf<String, Long>(
      "2016.2.1" to 201621,
      "2020.3.9999" to 202039,
      "8.0.1000" to 809,
      "2022.20" to 202290
    )

    data.forEach { (k, v) ->
      val version = ReSharperVersion.fromString(k)

      Assert.assertEquals("ReSharper version (as long) is not as expected", v, version.asLong())
    }
  }
}