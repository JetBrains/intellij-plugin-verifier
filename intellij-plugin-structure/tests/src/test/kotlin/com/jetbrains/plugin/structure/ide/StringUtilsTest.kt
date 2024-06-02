package com.jetbrains.plugin.structure.ide

import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilsTest {
  @Test
  fun `common prefix for four items is found`() {
    val strings = listOf(
      "plugins/javaee-app-servers-impl/lib/javaee-app-servers-impl.jar",
      "plugins/javaee-app-servers-impl/lib/AppServersView.jar",
      "plugins/javaee-app-servers-impl/lib/javaee-appServers-rt.jar",
      "plugins/javaee-app-servers-impl/lib/javaee-appServers-cloud-rt.jar"
    )

    val commonPrefix = getCommonsPrefix(strings)
    assertEquals("plugins/javaee-app-servers-impl/lib/", commonPrefix)
  }

  @Test
  fun `common prefix for empty list`() {
    val strings = emptyList<String>()

    val commonPrefix = getCommonsPrefix(strings)
    assertEquals("", commonPrefix)
  }

  @Test
  fun `common prefix for single item`() {
    val strings = listOf("plugins/javaee-extensions/lib/javaee-extensions.jar")

    val commonPrefix = getCommonsPrefix(strings)
    assertEquals("plugins/javaee-extensions/lib/javaee-extensions.jar", commonPrefix)
  }

  @Test
  fun `common prefix for two equal strings`() {
    val strings = listOf(
      "plugins/javaee-extensions/lib/javaee-extensions.jar",
      "plugins/javaee-extensions/lib/javaee-extensions.jar",
    )

    val commonPrefix = getCommonsPrefix(strings)
    assertEquals("plugins/javaee-extensions/lib/javaee-extensions.jar", commonPrefix)
  }

  @Test
  fun `test various corner cases`() {
    assertEquals("", getCommonsPrefix(listOf("", "")))
    assertEquals("", getCommonsPrefix(listOf("", "abc")))
    assertEquals("", getCommonsPrefix(listOf("abc", "")))
    assertEquals("a", getCommonsPrefix(listOf("abc", "a")))
    assertEquals("ab", getCommonsPrefix(listOf("ab", "abxyz")))
    assertEquals("ab", getCommonsPrefix(listOf("abcde", "abxyz")))
    assertEquals("", getCommonsPrefix(listOf("abcde", "xyz")))
    assertEquals("", getCommonsPrefix(listOf("xyz", "abcde")))
    assertEquals("lorem ipsum ", getCommonsPrefix(listOf("lorem ipsum dolor", "lorem ipsum velit")))
  }


}