package com.jetbrains.pluginverifier.parameters.filtering.documented

import java.util.regex.PatternSyntaxException

/**
 * @author Sergey Patrikeev
 */
class DocumentedProblemsParser {
  fun parse(pageBody: String): List<DocumentedProblem> = pageBody
      .substringAfter("<!--VERIFIER", "").substringBefore("-->", "")
      .lines()
      .map { it.trim() }
      .filterNot { it.isEmpty() }
      .mapNotNull {
        try {
          Regex(it, RegexOption.IGNORE_CASE)
        } catch (e: PatternSyntaxException) {
          null
        }
      }
      .map { DocumentedProblem(it) }
}