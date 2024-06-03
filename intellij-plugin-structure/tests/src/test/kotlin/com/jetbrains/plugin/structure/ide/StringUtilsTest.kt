package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Path

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
  fun `various corner cases`() {
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

  @Test
  fun `common prefix as path`() {
    val classpath = classpath(
      "plugins/DatabaseTools/lib/database-plugin.jar",
      "plugins/DatabaseTools/lib/jdbc-console.jar",
      "plugins/DatabaseTools/lib/database-openapi.jar",
      "plugins/DatabaseTools/lib/aether-dependency-resolver.jar",
      "plugins/DatabaseTools/lib/maven-resolver-transport-file.jar",
      "plugins/DatabaseTools/lib/maven-resolver-transport-http.jar",
      "plugins/DatabaseTools/lib/maven-resolver-connector-basic.jar"
    )
    assertEquals(Path.of("plugins/DatabaseTools/lib"), commonParentDirectory(classpath))
  }

  @Test
  fun `common prefix for single-item classpath`() {
    val classpath = classpath(
      "plugins/DatabaseTools/lib/database-plugin.jar",
    )
    assertEquals(Path.of("plugins/DatabaseTools/lib/database-plugin.jar"), commonParentDirectory(classpath))
  }

  @Test
  fun `common prefix for empty classpath`() {
    val classpath = classpath()
    assertNull(commonParentDirectory(classpath))
  }

  @Test
  fun `common parent directory for four items is found`() {
    val paths = paths(
      "plugins/javaee-app-servers-impl/lib/javaee-app-servers-impl.jar",
      "plugins/javaee-app-servers-impl/lib/AppServersView.jar",
      "plugins/javaee-app-servers-impl/lib/javaee-appServers-rt.jar",
      "plugins/javaee-app-servers-impl/lib/javaee-appServers-cloud-rt.jar"
    )

    val commonPrefix = getCommonParentDirectory(paths)
    assertEqualPaths("plugins/javaee-app-servers-impl/lib", commonPrefix)
  }

  @Test
  fun `common parent directory for two items with common file prefix`() {
    val paths = paths(
      "plugins/testng/lib/testng-plugin.jar",
      "plugins/testng/lib/testng-rt.jar"
    )

    val commonPrefix = getCommonParentDirectory(paths)
    assertEqualPaths("plugins/testng/lib", commonPrefix)
  }

  @Test
  fun `common parent directory for empty list`() {
    val commonPrefix = getCommonParentDirectory(emptyList())
    assertNull(commonPrefix)
  }

  @Test
  fun `common parent directory for single item`() {
    val paths = paths("plugins/javaee-extensions/lib/javaee-extensions.jar")

    val commonPrefix = getCommonParentDirectory(paths)
    assertEqualPaths("plugins/javaee-extensions/lib/javaee-extensions.jar", commonPrefix)
  }

  @Test
  fun `common parent directory for two equal strings`() {
    val paths = paths(
      "plugins/javaee-extensions/lib/javaee-extensions.jar",
      "plugins/javaee-extensions/lib/javaee-extensions.jar",
    )

    val commonPrefix = getCommonParentDirectory(paths)
    assertEqualPaths("plugins/javaee-extensions/lib/javaee-extensions.jar", commonPrefix)
  }

  @Test
  fun `various corner cases for common parent directory`() {
    assertEqualPaths("a", getCommonParentDirectory(paths("a/b/c", "a")))
    assertEqualPaths("a/b", getCommonParentDirectory(paths("a/b", "a/b/x/y/z")))
    assertEqualPaths("a/b", getCommonParentDirectory(paths("a/b/c/d/e", "a/b/x/y/z")))
    assertNull("", getCommonParentDirectory(paths("a/b/c/d/e", "x/y/z")))
    assertNull("", getCommonParentDirectory(paths("x/y/z", "a/b/c/d/e")))
    assertEqualPaths("lorem/ipsum/", getCommonParentDirectory(paths("lorem/ipsum/dolor", "lorem/ipsum/velit")))
  }


  private fun assertEqualPaths(expected: String, path: Path?) {
    assertEquals(Path.of(expected), path)
  }

  private fun paths(vararg paths: String) = paths.map { Path.of(it) }

  private fun classpath(vararg paths: String): LayoutComponent.Classpathable {
    return object : LayoutComponent.Classpathable {
      override fun getClasspath(): List<Path> = paths.map {
        Path.of(it)
      }
    }
  }
}