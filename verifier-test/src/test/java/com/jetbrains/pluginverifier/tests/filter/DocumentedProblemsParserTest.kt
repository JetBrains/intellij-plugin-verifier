package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.pluginverifier.parameters.filtering.documented.*
import org.junit.Assert
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class DocumentedProblemsParserTest {

  private val parser: DocumentedProblemsParser = DocumentedProblemsParser()

  @Test
  fun parseTest() {
    val clazz = DocumentedProblemsParserTest::class.java
    val text = clazz.getResourceAsStream("/documentedProblems.md").bufferedReader().use { it.readText() }
    val documentedProblems: List<DocumentedProblem> = parser.parse(text)
    Assert.assertEquals(listOf(
        DocPackageRemoved("com/example/deletedPackage"),
        DocAbstractMethodAdded("com/example/Faz", "newAbstractMethod"),
        DocFieldRemoved("com/example/Baz", "REMOVED_FIELD"),
        DocClassRemoved("com/example/Foo"),
        DocMethodRemoved("com/example/Bar", "removedMethod"),
        DocClassMovedToPackage("com/example/Baf", "com/another")
    ), documentedProblems)
  }
}