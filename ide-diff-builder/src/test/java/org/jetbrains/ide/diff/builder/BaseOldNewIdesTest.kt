package org.jetbrains.ide.diff.builder

import java.io.File

abstract class BaseOldNewIdesTest {

  companion object {
    fun getOldIdeFile() = getMockIdesRoot().resolve("old-ide")

    fun getNewIdeFile() = getMockIdesRoot().resolve("new-ide")

    private fun getMockIdesRoot(): File {
      val testDataRoot = File("build/mock-ides")
      if (testDataRoot.isDirectory) {
        return testDataRoot
      }
      return File("ide-diff-builder").resolve(testDataRoot).also {
        check(it.isDirectory)
      }
    }
  }

}