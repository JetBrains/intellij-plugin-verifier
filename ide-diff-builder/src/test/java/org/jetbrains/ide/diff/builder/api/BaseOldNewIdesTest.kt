package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.ide.IdeManager
import org.jetbrains.ide.diff.builder.filter.PackagesClassFilter
import org.jetbrains.ide.diff.builder.ide.IdeDiffBuilder
import org.junit.Assert
import java.nio.file.Path
import java.nio.file.Paths

abstract class BaseOldNewIdesTest {

  companion object {
    fun getOldIdeFile() = getMockIdesRoot().resolve("old-ide")

    fun getNewIdeFile() = getMockIdesRoot().resolve("new-ide")

    private fun getMockIdesRoot(): Path {
      val testDataRoot = Paths.get("build").resolve("mock-ides")
      if (testDataRoot.isDirectory) {
        return testDataRoot
      }
      return Paths.get("ide-diff-builder").resolve(testDataRoot).also {
        check(it.isDirectory)
      }
    }
  }

  fun buildApiReport(): ApiReport {
    val oldIdeFile = getOldIdeFile()
    val newIdeFile = getNewIdeFile()

    val oldIde = IdeManager.createManager().createIde(oldIdeFile)
    val newIde = IdeManager.createManager().createIde(newIdeFile)

    val jdkHome = getJdkPathForTests()

    return IdeDiffBuilder(PackagesClassFilter(emptyList()), jdkHome).buildIdeDiff(
      oldIde = oldIde,
      newIde = newIde,
      shouldBuildOldIdeDeprecatedApis = true
    )
  }

  private fun getJdkPathForTests(): Path {
    val javaHome = System.getenv("JAVA_HOME")?.let { Paths.get(it) }
    if (javaHome != null && javaHome.exists()) {
      return javaHome
    }

    val jvmHomeDir = Paths.get("/usr/lib/jvm")
    if (jvmHomeDir.exists()) {
      val someJdk = jvmHomeDir.listFiles().firstOrNull { it.isDirectory }
      if (someJdk != null) {
        println("Using $someJdk as JDK in tests")
        return someJdk
      }
    }

    throw IllegalArgumentException("No suitable JDK is found for the test")
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