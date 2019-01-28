package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.ide.diff.builder.BaseOldNewIdesTest
import org.junit.Assert.assertEquals
import org.junit.Test

class IdeDiffBuilderTest : BaseOldNewIdesTest() {

  private val accessOpenedClasses = listOf(
      "access.AccessOpenedClass",
      "access.AccessOpenedContent.PrivateInnerBecameProtected",
      "access.AccessOpenedContent.PrivateInnerBecamePublic",
      "access.AccessOpenedContent.PrivateNestedBecameProtected",
      "access.AccessOpenedContent.PrivateNestedBecamePublic"
  )

  private val accessClosedClasses = listOf(
      "access.AccessClosedClass",
      "access.AccessClosedContent.ProtectedInnerBecamePrivate",
      "access.AccessClosedContent.ProtectedNestedBecamePrivate",
      "access.AccessClosedContent.PublicInnerBecamePrivate",
      "access.AccessClosedContent.PublicNestedBecamePrivate"
  )

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

  private val accessOpenedMethods = listOf(
      "access.AccessOpenedContent void privateMethodBecamePublic()",
      "access.AccessOpenedContent void privateMethodBecameProtected()"
  )

  private val accessClosedMethods = listOf(
      "access.AccessClosedContent void publicMethodBecamePrivate()",
      "access.AccessClosedContent void protectedMethodBecamePrivate()"
  )

  private val newFields = listOf(
      "added.AddedContent publicField",
      "added.AddedContent protectedField"
  )

  private val removedFields = listOf(
      "removed.RemovedContent publicField",
      "removed.RemovedContent protectedField"
  )

  private val accessOpenedFields = listOf(
      "access.AccessOpenedContent privateFieldBecamePublic",
      "access.AccessOpenedContent privateFieldBecameProtected"
  )

  private val accessClosedFields = listOf(
      "access.AccessClosedContent publicFieldBecamePrivate",
      "access.AccessClosedContent protectedFieldBecamePrivate"
  )

  @Test
  fun `run diff builder for old and new IDEs`() {
    val apiReport = buildApiReport()
    val ideVersion = IdeVersion.createIdeVersion("2.0")
    val introducedIn = IntroducedIn(ideVersion)
    val removedIn = RemovedIn(ideVersion)
    assertEquals(setOf(introducedIn, removedIn), apiReport.apiEventToData.keys)

    val introducedApiData = apiReport.apiEventToData[introducedIn]!!
    assertSetsEqual(
        (newClasses + newMethods + newFields + accessOpenedClasses + accessOpenedMethods + accessOpenedFields).toSet(),
        introducedApiData.apiSignatures.map { it.externalPresentation }.toSet()
    )

    val removedInData = apiReport.apiEventToData[removedIn]!!
    assertSetsEqual(
        (removedClasses + removedMethods + removedFields + accessClosedClasses + accessClosedMethods + accessClosedFields).toSet(),
        removedInData.apiSignatures.map { it.externalPresentation }.toSet()
    )
  }

}