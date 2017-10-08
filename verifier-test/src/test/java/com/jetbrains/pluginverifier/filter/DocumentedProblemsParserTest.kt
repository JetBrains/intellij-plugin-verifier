package com.jetbrains.pluginverifier.filter

import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblem
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblemsParser
import org.junit.Assert
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class DocumentedProblemsParserTest {

  private val parser: DocumentedProblemsParser = DocumentedProblemsParser()

  @Test
  fun parseTest() {
    val text = DocumentedProblemsParserTest::class.java.getResourceAsStream("/documentedProblems.md").bufferedReader().use { it.readText() }
    val documentedProblems: List<DocumentedProblem> = parser.parse(text)
    Assert.assertEquals(listOf(
        DocumentedProblem.PackageRemoved("com/example/deletedPackage"),
        DocumentedProblem.AbstractMethodAdded("com/example/Faz", "newAbstractMethod"),
        DocumentedProblem.FieldRemoved("com/example/Baz", "REMOVED_FIELD"),
        DocumentedProblem.ClassRemoved("com/example/Foo"),
        DocumentedProblem.MethodRemoved("com/example/Bar", "removedMethod")
    ), documentedProblems)
  }
}