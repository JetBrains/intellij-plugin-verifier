package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.pluginverifier.filtering.documented.*
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsParser.Companion.toInternalName
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsParser.Companion.unwrapMarkdownTags
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class DocumentedProblemsParserTest {

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun `unwrap markdown tags test`() {
    assertEquals("nothing", unwrapMarkdownTags("nothing"))
    assertEquals("val x = 10", unwrapMarkdownTags("`val x = 10`"))
    assertEquals("com.example.deletedPackage", unwrapMarkdownTags("`com.example.deletedPackage`"))
    assertEquals("com.example.deletedPackage", unwrapMarkdownTags("`com.example.deletedPackage`"))
    assertEquals("com.example.Foo\$InnerClass.changedReturnType(int, String) method return type changed from One to Another", unwrapMarkdownTags("`com.example.Foo\$InnerClass.changedReturnType(int, String)` method return type changed from `One` to `Another`"))
  }

  @Test
  fun `parse dot class name`() {
    assertEquals("org/some/Class", toInternalName("org.some.Class"))
    assertEquals("com/example/Inner\$Class", toInternalName("com.example.Inner.Class"))
    assertEquals("com/somePackage/SomeClass", toInternalName("com.somePackage.SomeClass"))
    assertEquals("com/some/class", toInternalName("com.some.class"))
    assertEquals("DefaultPackageClassName", toInternalName("DefaultPackageClassName"))
  }

  @Test
  fun parseTest() {
    val clazz = DocumentedProblemsParserTest::class.java
    val pageBody = clazz.getResourceAsStream("/exampleDocumentedProblems.md").bufferedReader().use { it.readText() }
    val documentedProblems: List<DocumentedProblem> = DocumentedProblemsParser(false).parse(pageBody)
    val expectedProblems = listOf(
      DocPackageRemoved("com/example/deletedPackage"),
      DocAbstractMethodAdded("com/example/Faz", "newAbstractMethod"),
      DocAbstractMethodAdded("com/example/SomeClass", "abstractMethodWithParams"),
      DocAbstractMethodAdded("com/example/MethodHolder", "abstractMethodSeparatedWithDash"),
      DocAbstractMethodAdded("com/some/Class", "markedAbstractMethod"),
      DocFieldRemoved("com/example/Baz", "REMOVED_FIELD"),
      DocClassRemoved("com/example/Foo"),
      DocClassRemoved("com/example/SomeInterface"),
      DocClassRemoved("com/example/SomeAnnotation"),
      DocClassRemoved("com/example/SomeEnum"),
      DocMethodRemoved("com/example/Bar", "removedMethod"),
      DocMethodRemoved("com/example/RemovedConstructorWithParams", "<init>"),
      DocClassMovedToPackage("com/example/Baf", "com/another"),
      DocClassMovedToPackage("com/example/MI", "com/another"),
      DocClassMovedToPackage("com/example/MA", "com/another"),
      DocClassMovedToPackage("com/example/ME", "com/another"),
      DocMethodReturnTypeChanged("com/example/Foo\$InnerClass", "changedReturnType"),
      DocMethodParameterTypeChanged("com/example/Bar", "changedParameterType"),
      DocMethodVisibilityChanged("com/example/Bar", "methodVisibilityChanged"),
      DocFieldTypeChanged("com/example/Foo", "fieldTypeChanged"),
      DocFieldVisibilityChanged("com/example/Foo", "fieldVisibilityChanged"),
      DocClassRemoved("com/example/Inner\$Class"),
      DocClassRemoved("com/example/Bam"),
      DocClassRemoved("com/example/Iface"),
      DocClassRemoved("com/example/Annotation"),
      DocClassRemoved("com/example/Enum"),
      DocMethodParameterTypeChanged("com/example/Foo", "foo"),
      DocMethodRemoved("com/example/Baz", "<init>"),
      DocMethodParameterTypeChanged("com/example/Baf", "<init>"),
      DocMethodParameterTypeChanged("com/example/Bam", "<init>"),
      DocMethodMarkedFinal("com/example/MethodHolder", "methodMarkedFinal"),
      DocFinalMethodInherited("com/some/Class", "com/some/other/Class", "methodName"),
      DocAbstractMethodAdded("com/some/Class", "abstractMethod"),
      DocMethodParameterMarkedWithAnnotation("com/some/Class", "someMarkedMethod", "com/some/Parameter", "com/some/Annotation"),
      DocClassTypeParameterAdded("com/some/Class"),
      DocSuperclassChanged("com/some/Class", "com/some/old/Super", "com/some/new/Super"),
      DocSuperclassChanged("com/some/Interface", "com/some/old/SuperIface", "com/some/new/NewSuperIface"),
      DocPropertyRemoved("some.property.name", "messages.Bundle")
    )
    for (expected in expectedProblems) {
      assertTrue("$expected is not found:\n${documentedProblems.joinToString("\n")}\nActual problems:", expected in documentedProblems)
    }
    val redundant = documentedProblems - expectedProblems
    if (redundant.isNotEmpty()) {
      fail("Redundant:\n" + redundant.joinToString("\n"))
    }
  }

  @Test
  fun `failed to parse`() {
    expectedEx.expect(DocumentedProblemsParseException::class.java)
    val page = """
  `SomeClass` non-parsed description 
  : Use classes from `org.apache.commons.imaging` instead
""".trimIndent()
    DocumentedProblemsParser(false).parse(page)
  }

  @Test
  fun `ignore non parsed`() {
    val page = """
      non-parsed description 
      : Use classes from `org.apache.commons.imaging` instead
      
      `com.some.Class` class removed
      : reason
    """.trimIndent()
    val documentedProblems = DocumentedProblemsParser(true).parse(page)
    assertEquals(listOf(DocClassRemoved("com/some/Class")), documentedProblems)
  }

}