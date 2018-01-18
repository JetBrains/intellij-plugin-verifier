package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Implementation of the [ProblemsFilter] that drops
 * the problems documented on the
 * [Breaking API Changes page](http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 */
class DocumentedProblemsFilter(private val documentedProblems: List<DocumentedProblem>) : ProblemsFilter {

  override fun shouldReportProblem(plugin: IdePlugin, ideVersion: IdeVersion, problem: CompatibilityProblem, verificationContext: VerificationContext): ProblemsFilter.Result {
    val documentedProblem = documentedProblems.find { it.isDocumenting(problem, verificationContext) }
    if (documentedProblem != null) {
      return ProblemsFilter.Result.Ignore("the problem is already documented in the API Breakages page (http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html)")
    }
    return ProblemsFilter.Result.Report
  }

}