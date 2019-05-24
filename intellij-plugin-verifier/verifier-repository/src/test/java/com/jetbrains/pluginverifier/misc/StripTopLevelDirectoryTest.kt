package com.jetbrains.pluginverifier.misc

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StripTopLevelDirectoryTest {

  @JvmField
  @Rule
  val tempFolder = TemporaryFolder()

  /**
   * f
   */
  @Test
  fun `empty directory`() {
    val tmpFolder = tempFolder.newFolder()
    stripTopLevelDirectory(tmpFolder.toPath())
    assertArrayEquals(emptyArray(), tmpFolder.list())
  }

  /**
   * f
   *   a
   *   b
   */
  @Test
  fun `more than one file`() {
    val tmpFolder = tempFolder.newFolder()
    tmpFolder.resolve("a").createDir()
    tmpFolder.resolve("b").createDir()
    stripTopLevelDirectory(tmpFolder.toPath())
    assertEquals(listOf("a", "b"), tmpFolder.list().sorted())
  }

  /**
   * f
   *   s
   *     a
   *     b
   */
  @Test
  fun simple() {
    val tmpFolder = tempFolder.newFolder()
    val s = tmpFolder.resolve("s").createDir()
    s.resolve("a").createDir()
    s.resolve("b").writeText("42")
    stripTopLevelDirectory(tmpFolder.toPath())
    assertEquals(listOf("a", "b"), tmpFolder.list().sorted())
  }

  /**
   * f
   *   s
   *     a
   *     s/b.txt
   *
   * should be
   * f
   *   a
   *   s/b.txt
   */
  @Test
  fun conflict() {
    val tmpFolder = tempFolder.newFolder()
    val s = tmpFolder.resolve("s").createDir()
    s.resolve("a").createDir()

    val ss = s.resolve("s").createDir()
    ss.resolve("b.txt").writeText("42")
    stripTopLevelDirectory(tmpFolder.toPath())
    assertEquals(listOf("a", "s"), tmpFolder.list().sorted())

    val moved = tmpFolder.resolve("s").resolve("b.txt")
    assertTrue(moved.exists())
    assertEquals("42", moved.readText())
  }
}