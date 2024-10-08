package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.jetbrains.plugin.structure.base.utils.exists
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

class ContentBuilderTest {
  @Test
  fun `single directory is properly constructed`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      val resultPath = buildDirectory(rootDir) {
        dirs("jetbrains") {
          file("build.txt", "IU-243.12818.78")
        }
      }
      Files.walk(resultPath).use { it.toList() }.run { println(this) }
      val expectedPath = jimFs.getPath("/jetbrains/build.txt")
      assertTrue(expectedPath.exists())
    }
  }

  @Test
  fun `single directory with empty content is properly constructed`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      buildDirectory(rootDir) {
        dirs("jetbrains") { /* empty directory */ }
      }
      val expectedPath = jimFs.getPath("/jetbrains")
      assertTrue(expectedPath.exists())
    }
  }

  @Test
  fun `directory with a subdirectory that contains an empty content is properly constructed`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      buildDirectory(rootDir) {
        dirs("jetbrains/ide") { /* empty directory */ }
      }
      val expectedPath = jimFs.getPath("/jetbrains/ide")
      assertTrue(expectedPath.exists())
    }
  }

  @Test
  fun `directories are properly constructed`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      val resultPath = buildDirectory(rootDir) {
        dirs("jetbrains/ide/idea") {
          file("build.txt", "IU-243.12818.78")
        }
      }
      Files.walk(resultPath).use { it.toList() }.run { println(this) }
      val expectedPath = jimFs.getPath("/jetbrains/ide/idea/build.txt")
      assertTrue(expectedPath.exists())
    }
  }

  @Test
  fun `directories are properly constructed with deeply nested directories`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      val resultPath = buildDirectory(rootDir) {
        dirs("jetbrains/products/ide/idea") {
          file("build.txt", "IU-243.12818.78")
        }
      }
      Files.walk(resultPath).use { it.toList() }.run { println(this) }
      val expectedPath = jimFs.getPath("/jetbrains/products/ide/idea/build.txt")
      assertTrue(expectedPath.exists())
    }
  }

  @Test
  fun `directories are properly constructed with 5 layers of nested directories`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      val resultPath = buildDirectory(rootDir) {
        dirs("dev/jetbrains/products/ide/idea") {
          file("build.txt", "IU-243.12818.78")
        }
      }
      Files.walk(resultPath).use { it.toList() }.run { println(this) }
      val expectedPath = jimFs.getPath("/dev/jetbrains/products/ide/idea/build.txt")
      assertTrue(expectedPath.exists())
    }
  }

  @Test
  fun `directories are properly constructed with single dir`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      buildDirectory(rootDir) {
        dirs("idea") {
          dir("lib") {
            file("idea_rt.jar")
          }
        }
      }
      val expectedPath = jimFs.getPath("/idea/lib/idea_rt.jar")
      assertTrue(expectedPath.exists())
    }
  }

  @Test
  fun `directories are properly constructed with deeply nested directories and explicitly nested directory`() {
    Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
      val rootDir: Path = jimFs.rootDirectories.first()

      val resultPath = buildDirectory(rootDir) {
        dirs("idea/lib") {
          dir("rt") {
            file("servlet.jar")
          }
        }
      }
      Files.walk(resultPath).use { it.toList() }.run { println(this) }
      val expectedPath = jimFs.getPath("/idea/lib/rt/servlet.jar")
      assertTrue(expectedPath.exists())
    }
  }
}