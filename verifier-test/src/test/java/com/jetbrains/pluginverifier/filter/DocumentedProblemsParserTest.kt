package com.jetbrains.pluginverifier.filter

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
        "Access to unresolved class org.apache.sanselan..*",
        "Access to unresolved class org.jetbrains.asm4..*",
        "Abstract method com.intellij.openapi.application.ApplicationListener.afterWriteActionFinished.* is not implemented",
        "Access to unresolved field com.intellij.util.net.HttpConfigurable.PROXY_LOGIN.*",
        "Access to unresolved field com.intellij.util.net.HttpConfigurable.PROXY_PASSWORD_CRYPT.*"
    ), documentedProblems.map { it.problemRegex.pattern })
  }
}