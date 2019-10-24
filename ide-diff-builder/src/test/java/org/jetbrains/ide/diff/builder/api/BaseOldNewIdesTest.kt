package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.ide.IdeManager
import org.jetbrains.ide.diff.builder.filter.PackagesClassFilter
import org.jetbrains.ide.diff.builder.ide.IdeDiffBuilder
import org.junit.Assert
import java.io.File
import java.nio.file.Paths

abstract class BaseOldNewIdesTest {

  companion object {
    fun getOldIdeFile() = getMockIdesRoot().resolve("old-ide")

    fun getNewIdeFile() = getMockIdesRoot().resolve("new-ide")

    private fun getMockIdesRoot(): File {
      val testDataRoot = File("build").resolve("mock-ides")
      if (testDataRoot.isDirectory) {
        return testDataRoot
      }
      return File("ide-diff-builder").resolve(testDataRoot).also {
        check(it.isDirectory)
      }
    }
  }

  fun buildApiReport(): ApiReport {
    val oldIdeFile = getOldIdeFile()
    val newIdeFile = getNewIdeFile()

    val oldIde = IdeManager.createManager().createIde(oldIdeFile)
    val newIde = IdeManager.createManager().createIde(newIdeFile)

    val javaHome = System.getenv("JAVA_HOME")
    requireNotNull(javaHome) { "JAVA_HOME is not specified" }
    val jdkPath = Paths.get(javaHome)

    return IdeDiffBuilder(PackagesClassFilter(emptyList()), jdkPath).buildIdeDiff(oldIde, newIde)
  }

  fun <T> assertSetsEqual(expected: Set<T>, actual: Set<T>) {
    val redundant = (actual - expected)
    val absent = (expected - actual)

    if (redundant.isNotEmpty()) {
      println("Redundant")
      for (s in redundant) {
        println("  $s")
      }
    }

    if (absent.isNotEmpty()) {
      println("Absent")
      for (s in absent) {
        println("  $s")
      }
    }

    Assert.assertTrue(redundant.isEmpty() && absent.isEmpty())
  }

}