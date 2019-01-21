package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.BaseOldNewIdesTest
import org.junit.Assert.assertEquals
import org.junit.Test

class IdeDiffBuilderTest : BaseOldNewIdesTest() {

  private val newClasses = listOf(
      "added.AddedClass",
      "added.AddedContent.PublicInner",
      "added.AddedContent.ProtectedInner",
      "added.AddedContent.PublicNested",
      "added.AddedContent.ProtectedNested"
  )

  private val removedClasses = listOf(
      "removed.RemovedClass",
      "removed.RemovedContent.PublicInner",
      "removed.RemovedContent.ProtectedInner",
      "removed.RemovedContent.PublicNested",
      "removed.RemovedContent.ProtectedNested"
  )

  private val newMethods = listOf(
      "added.AddedContent AddedContent(int)",
      "added.AddedContent void addedPublicMethod()",
      "added.AddedContent void addedProtectedMethod()",
      "added.AddedContent void addedPublicStaticMethod()",
      "added.AddedContent void addedProtectedStaticMethod()",

      "added.AddedContent void methodWithGenericParameter(java.lang.Number)",
      "added.AddedContent java.lang.Number methodWithGenericReturnType()"
  )

  private val removedMethods = listOf(
      "removed.RemovedContent void publicMethod()",
      "removed.RemovedContent void publicStaticMethod()"
  )

  private val newFields = listOf(
      "added.AddedContent publicField",
      "added.AddedContent protectedField"
  )

  private val removedFields = listOf(
      "removed.RemovedContent publicField",
      "removed.RemovedContent protectedField"
  )

  @Test
  fun `run diff builder for old and new IDEs`() {
    val apiReport = buildApiReport()
    val ideVersion = IdeVersion.createIdeVersion("2.0")
    val introducedIn = IntroducedIn(ideVersion)
    val removedIn = RemovedIn(ideVersion)
    assertEquals(setOf(introducedIn, removedIn), apiReport.apiEventToData.keys)

    val introducedApiData = apiReport.apiEventToData[introducedIn]!!
    assertEquals(
        (newClasses + newMethods + newFields).sorted(),
        introducedApiData.apiSignatures.map { it.externalPresentation }.sorted()
    )

    val removedInData = apiReport.apiEventToData[removedIn]!!
    assertEquals(
        (removedClasses + removedMethods + removedFields).sorted(),
        removedInData.apiSignatures.map { it.externalPresentation }.sorted()
    )
  }

}