package com.jetbrains.pluginverifier.filter

import com.jetbrains.pluginverifier.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class DocumentedProblem(val problemRegex: Regex) {

  fun isDocumenting(problem: Problem): Boolean = problemRegex.matches(problem.shortDescription)

}