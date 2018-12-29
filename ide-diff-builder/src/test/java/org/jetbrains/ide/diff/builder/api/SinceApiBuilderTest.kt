package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
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

      return SinceApiBuilder(interestingPackages = listOf("added")).build(oldIde, newIde)
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
      "added.A.B",
      "added.A.C"
  )

  private val expectedNewMethods = listOf(
      "added.A A()",
      "added.A void m1()",
      "added.A void m2()",
      "added.A void m3(java.util.Map<java.lang.Integer,java.lang.Integer>)",

      "added.A.B B()",
      "added.A.C C()"
  )

  private val expectedNewFields = listOf(
      "added.A f1",
      "added.A f2"
  )

  private fun checkSinceApi(sinceApiData: SinceApiData) {
    val apiData = sinceApiData.versionToApiData.values.first()
    assertEquals(
        (expectedNewClasses + expectedNewMethods + expectedNewFields).sorted(),
        apiData.apiSignatures.map { it.externalPresentation }.sorted()
    )
  }

}