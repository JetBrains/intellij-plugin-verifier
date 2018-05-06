package com.jetbrains.pluginverifier.tasks.common

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.tasks.TaskResult

data class VerificationResultComparison(val plugin: PluginInfo,
                                        val oldResult: VerificationResult,
                                        val newResult: VerificationResult,
                                        val newProblems: Set<CompatibilityProblem>,
                                        val oldDirectMissingDependencies: List<MissingDependency>,
                                        val newDirectMissingDependencies: List<MissingDependency>)

class NewProblemsResult(val baseTarget: VerificationTarget,
                        val baseResults: List<VerificationResult>,
                        val newTarget: VerificationTarget,
                        val newResults: List<VerificationResult>,
                        val resultsComparisons: Map<PluginInfo, VerificationResultComparison>) : TaskResult() {

  companion object {
    fun create(baseTarget: VerificationTarget,
               baseResults: List<VerificationResult>,
               newTarget: VerificationTarget,
               newResults: List<VerificationResult>): NewProblemsResult {
      val basePlugin2Result = baseResults.associateBy { it.plugin }
      val newPlugin2Result = newResults.associateBy { it.plugin }

      val resultsComparisons = hashMapOf<PluginInfo, VerificationResultComparison>()

      for ((plugin, newResult) in newPlugin2Result) {
        val baseResult = basePlugin2Result[plugin] ?: continue
        val newProblems = getNewProblems(baseResult, newResult)

        val oldDirectMissingDeps = baseResult.getDirectMissingDependencies()
        val newDirectMissingDeps = newResult.getDirectMissingDependencies()

        if (shouldSkipResult(baseResult, newResult)) {
          continue
        }
        resultsComparisons[plugin] = VerificationResultComparison(
            plugin,
            baseResult,
            newResult,
            newProblems,
            oldDirectMissingDeps,
            newDirectMissingDeps
        )
      }

      return NewProblemsResult(baseTarget, baseResults, newTarget, newResults, resultsComparisons)
    }

    /**
     * Determines whether it is necessary to skip comparison of [baseResult] and [newResult]
     * due to mismatch in plugins that were available at the verification time.
     *
     * For example, if a plugin to be verified could have been resolved when we verified the [releaseIdeVersion],
     * and later it became unavailable when we verified the [trunkIdeVersion],
     * we should skip that plugin to avoid false-positives.
     */
    private fun shouldSkipResult(baseResult: VerificationResult,
                                 newResult: VerificationResult): Boolean {
      if (baseResult is VerificationResult.NotFound ||
          baseResult is VerificationResult.FailedToDownload ||
          newResult is VerificationResult.NotFound ||
          newResult is VerificationResult.FailedToDownload) {
        return true
      }
      return false
    }

    /**
     * Determines which problems appeared against the new target
     * comparing to the base target, with respect to possible
     * problems transformations.
     */
    private fun getNewProblems(oldResult: VerificationResult, newResult: VerificationResult): Set<CompatibilityProblem> {
      val oldProblems = oldResult.getProblems()
      val newProblems = newResult.getProblems()
      return newProblems.filterNotTo(hashSetOf()) { isOldProblem(it, oldProblems) }
    }

    private fun VerificationResult.getDirectMissingDependencies() = when (this) {
      is VerificationResult.MissingDependencies -> directMissingDependencies
      else -> emptyList()
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
      is VerificationResult.MissingDependencies -> compatibilityProblems
      is VerificationResult.CompatibilityProblems -> compatibilityProblems
    }

  }

}
