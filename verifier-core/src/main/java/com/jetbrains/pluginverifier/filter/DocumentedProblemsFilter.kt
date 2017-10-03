package com.jetbrains.pluginverifier.filter

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.problems.Problem
import java.io.File

class DocumentedProblemsFilter(private val documentedProblems: List<DocumentedProblem>,
                               private val documentedIgnoredProblemsFile: File?) : ProblemsFilter() {

  private val ignoredProblems = arrayListOf<Triple<IdePlugin, Problem, DocumentedProblem>>()

  override fun accept(plugin: IdePlugin, problem: Problem): Boolean {
    val documentedProblem = documentedProblems.find { it.isDocumenting(problem) }
    if (documentedProblem != null) {
      ignoredProblems.add(Triple(plugin, problem, documentedProblem))
    }
    return documentedProblem == null
  }

  override fun onClose() {
    documentedIgnoredProblemsFile?.bufferedWriter()?.use { writer ->
      for ((plugin, problem, documentedProblem) in ignoredProblems) {
        writer.appendln(getIgnoredProblemLine(plugin, documentedProblem, problem))
      }
    }
  }

  private fun getIgnoredProblemLine(plugin: IdePlugin, documentedProblem: DocumentedProblem, problem: Problem) =
      "Problem of the plugin $plugin is already documented on the http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html " +
          "by $documentedProblem:\n#${problem.fullDescription}"
}