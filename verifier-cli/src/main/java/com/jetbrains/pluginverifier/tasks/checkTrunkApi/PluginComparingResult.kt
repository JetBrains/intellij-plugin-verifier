package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.*

/**
 * @author Sergey Patrikeev
 */
data class PluginComparingResult(val plugin: PluginInfo,
                                 val releaseResult: Result,
                                 val trunkResult: Result) {

  /**
   * Determines the problems of the Trunk IDE which were not present in the Release IDE
   * with respect to possible problems transformations.
   */
  fun getNewApiProblems(): Set<Problem> {
    val releaseProblems = releaseResult.verdict.getProblems()
    val trunkProblems = trunkResult.verdict.getProblems()
    return trunkProblems.filterNotTo(hashSetOf()) { isOldProblem(it, releaseProblems) }
  }

  /**
   * Determines whether the [problem] is present in [oldProblems]
   * or is equivalent to any of the [oldProblems].
   */
  private fun isOldProblem(problem: Problem, oldProblems: Set<Problem>): Boolean {
    if (problem in oldProblems) {
      return true
    }

    return when (problem) {
      is MethodNotFoundProblem -> {
        /*
        Problem "Method is not accessible" changed to "Method is not found":
        It is the case when, for example, the private method was removed.
        The plugins invoking the private method had been already broken, so deletion
        of the method doesn't lead to "new" API breakages.
        */
        oldProblems.any { it is IllegalMethodAccessProblem && it.bytecodeMethodReference == problem.unresolvedMethod }
      }
      is IllegalMethodAccessProblem -> {
        /*
        Problem "Method is not found" changed to "Method is not accessible":
        It is the case when, for example, the method was removed, and then re-added with weaker access modifier.
        The plugins invoking the missing method will fail with "Access Error",
        so this is not the "new" API breakage.
        */
        oldProblems.any { it is MethodNotFoundProblem && it.unresolvedMethod == problem.bytecodeMethodReference } ||
            oldProblems.any { it is IllegalMethodAccessProblem && it.bytecodeMethodReference == problem.bytecodeMethodReference }
      }
      is IllegalFieldAccessProblem -> {
        /*
        Problem "Field is not found" changed to "Field is not accessible":
        This is similar to the method's case.
         */
        oldProblems.any { it is FieldNotFoundProblem && it.unresolvedField == problem.fieldBytecodeReference } ||
            oldProblems.any { it is IllegalFieldAccessProblem && it.fieldBytecodeReference == problem.fieldBytecodeReference }
      }
      is FieldNotFoundProblem -> {
        /*
        Problem "Field is not accessible" changed to "Field is not found"
        This is similar to deletion of a inaccessible method.
         */
        oldProblems.any { it is IllegalFieldAccessProblem && it.fieldBytecodeReference == problem.unresolvedField }
      }
      else -> false
    }
  }

  private fun Verdict.getProblems() = when (this) {
    is Verdict.NotFound,
    is Verdict.Bad,
    is Verdict.OK,
    is Verdict.Warnings,
    is Verdict.FailedToDownload -> emptySet()
    is Verdict.MissingDependencies -> problems
    is Verdict.Problems -> problems
  }

}