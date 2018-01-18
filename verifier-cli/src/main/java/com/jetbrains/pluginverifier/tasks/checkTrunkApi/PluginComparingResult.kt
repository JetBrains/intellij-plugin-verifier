package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.*

data class PluginComparingResult(val plugin: PluginInfo,
                                 val releaseResult: VerificationResult,
                                 val trunkResult: VerificationResult) {

  /**
   * Determines the problems of the Trunk IDE which were not present in the Release IDE
   * with respect to possible problems transformations.
   */
  fun getNewApiProblems(): Set<CompatibilityProblem> {
    val releaseProblems = releaseResult.getProblems()
    val trunkProblems = trunkResult.getProblems()
    return trunkProblems.filterNotTo(hashSetOf()) { isOldProblem(it, releaseProblems) }
  }

  /**
   * Determines whether the [problem] is present in [oldProblems]
   * or is equivalent to any of the [oldProblems].
   */
  private fun isOldProblem(problem: CompatibilityProblem, oldProblems: Set<CompatibilityProblem>): Boolean {
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

  private fun VerificationResult.getProblems() = when (this) {
    is VerificationResult.NotFound,
    is VerificationResult.InvalidPlugin,
    is VerificationResult.OK,
    is VerificationResult.StructureWarnings,
    is VerificationResult.FailedToDownload -> emptySet()
    is VerificationResult.MissingDependencies -> problems
    is VerificationResult.CompatibilityProblems -> problems
  }

}