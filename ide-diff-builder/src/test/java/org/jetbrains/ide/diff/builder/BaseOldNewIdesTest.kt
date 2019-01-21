package org.jetbrains.ide.diff.builder

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.api.IdeDiffBuilder
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

  fun buildApiReport(): ApiReport {
    val oldIdeFile = getOldIdeFile()
    val newIdeFile = getNewIdeFile()

    val oldIde = IdeManager.createManager().createIde(oldIdeFile)
    val newIde = IdeManager.createManager().createIde(newIdeFile)

    val jdkPath = JdkPath.createJavaHomeJdkPath()
    return IdeDiffBuilder(emptyList(), jdkPath).build(oldIde, newIde)
  }

}