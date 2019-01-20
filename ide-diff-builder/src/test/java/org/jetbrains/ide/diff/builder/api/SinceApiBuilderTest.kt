package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import org.jetbrains.ide.diff.builder.BaseOldNewIdesTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SinceApiBuilderTest : BaseOldNewIdesTest() {

  companion object {
    fun buildSinceApi(): SinceApiData {
      val oldIdeFile = getOldIdeFile()
      val newIdeFile = getNewIdeFile()

      val oldIde = IdeManager.createManager().createIde(oldIdeFile)
      val newIde = IdeManager.createManager().createIde(newIdeFile)

      val jdkPath = JdkPath.createJavaHomeJdkPath()
      return SinceApiBuilder(interestingPackages = listOf("added", "ignored"), jdkPath = jdkPath).build(oldIde, newIde)
    }
  }

  @Test
  fun `run diff builder for old and new IDEs`() {
    val sinceApiData = buildSinceApi()
    assertEquals(setOf(IdeVersion.createIdeVersion("2.0")), sinceApiData.versionToApiData.keys)
    checkSinceApi(sinceApiData)
  }

  private val expectedNewClasses = listOf(
      "added.A",
      "added.M.B",
      "added.M.C"
  )

  private val expectedNewMethods = listOf(
      "added.M M(int)",
      "added.M void m1()",
      "added.M void m2()",
      "added.M void m3(java.util.Map<java.lang.Integer,java.lang.Integer>)",

      "ignored.B void foo(java.lang.Number)"
  )

  private val expectedNewFields = listOf(
      "added.M f1",
      "added.M f2"
  )

  private fun checkSinceApi(sinceApiData: SinceApiData) {
    val apiData = sinceApiData.versionToApiData.values.first()
    assertEquals(
        (expectedNewClasses + expectedNewMethods + expectedNewFields).sorted(),
        apiData.apiSignatures.map { it.externalPresentation }.sorted()
    )
  }

}