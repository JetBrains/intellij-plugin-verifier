package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocumentedProblem
import com.jetbrains.pluginverifier.results.problems.Problem

class DocumentedProblemsFilter(private val documentedProblems: List<DocumentedProblem>) : ProblemsFilter {

  override fun shouldReportProblem(plugin: IdePlugin, ideVersion: IdeVersion, problem: Problem): ProblemsFilter.Result {
    val documentedProblem = documentedProblems.find { it.isDocumenting(problem) }
    if (documentedProblem != null) {
      return ProblemsFilter.Result.Ignore("the problem is already documented in the API Breakages page (http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html)\n $problem")
    }
    return ProblemsFilter.Result.Report
  }

}