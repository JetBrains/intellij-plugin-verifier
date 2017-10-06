package com.jetbrains.pluginverifier.parameters.filtering.documented

import com.jetbrains.pluginverifier.results.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class DocumentedProblem(val problemRegex: Regex) {

  fun isDocumenting(problem: Problem): Boolean = problemRegex.matches(problem.shortDescription)

}