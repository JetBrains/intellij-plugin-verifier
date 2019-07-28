package com.jetbrains.pluginverifier.filtering

import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblem
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsPagesFetcher
import com.jetbrains.pluginverifier.filtering.documented.DocumentedProblemsParser
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Implementation of the [ProblemsFilter] that drops
 * the problems documented on the
 * [Breaking API Changes page](https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 */
class DocumentedProblemsFilter(private val documentedProblems: List<DocumentedProblem>) : ProblemsFilter {

  override fun shouldReportProblem(problem: CompatibilityProblem, context: VerificationContext): ProblemsFilter.Result {
    val documentedProblem = documentedProblems.find { it.isDocumenting(problem, context) }
    if (documentedProblem != null) {
      return ProblemsFilter.Result.Ignore("the problem is already documented in the API Breakages page (https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html)")
    }
    return ProblemsFilter.Result.Report
  }

  companion object {

    private const val DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL = "https://raw.githubusercontent.com/JetBrains/intellij-sdk-docs/master/reference_guide/api_changes_list.md"

    /**
     * Accesses the [documentedProblemsPageUrl], parses it and returns the corresponding [DocumentedProblemsFilter].
     */
    fun createFilter(
        documentedProblemsPageUrl: String = DEFAULT_DOCUMENTED_PROBLEMS_PAGE_URL
    ): DocumentedProblemsFilter {
      val documentedPages = fetchDocumentedProblemsPages(documentedProblemsPageUrl)
      val documentedProblemsParser = DocumentedProblemsParser()
      val documentedProblems = documentedPages.flatMap { documentedProblemsParser.parse(it) }
      return DocumentedProblemsFilter(documentedProblems)
    }

    private fun fetchDocumentedProblemsPages(mainPageUrl: String) =
        DocumentedProblemsPagesFetcher().fetchPages(mainPageUrl)
  }

}