package com.jetbrains.pluginverifier.tasks.twoTargets

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.Browseable
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

class TwoTargetsResultPrinter(private val outputOptions: OutputOptions) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as TwoTargetsVerificationResults) {
      if (outputOptions.teamCityLog != null) {
        printResultsOnTeamCity(this, outputOptions.teamCityLog)
      } else {
        println("Enable TeamCity results printing option (-team-city or -tc) to see the results in TeamCity builds format.")
      }

      HtmlResultPrinter(baseTarget, outputOptions).printResults(baseResults)
      HtmlResultPrinter(newTarget, outputOptions).printResults(newResults)
    }
  }

  private fun printResultsOnTeamCity(twoTargetsVerificationResults: TwoTargetsVerificationResults, tcLog: TeamCityLog) {
    val allPlugin2Problems = HashMultimap.create<PluginInfo, CompatibilityProblem>()

    val pluginToTwoResults = twoTargetsVerificationResults.getPluginToTwoResults()
    for ((plugin, twoResults) in pluginToTwoResults) {
      allPlugin2Problems.putAll(plugin, twoResults.newProblems)
    }

    val allProblem2Plugins = Multimaps.invertFrom(allPlugin2Problems, HashMultimap.create<CompatibilityProblem, PluginInfo>())
    val allProblems = allProblem2Plugins.keySet()

    val baseTarget = twoTargetsVerificationResults.baseTarget
    val newTarget = twoTargetsVerificationResults.newTarget

    val newPluginIdToVerifications = ArrayListMultimap.create<String, PluginVerificationResult>()
    if (newTarget is PluginVerificationTarget.IDE) {
      for (result in twoTargetsVerificationResults.newResults) {
        newPluginIdToVerifications.put(result.plugin.pluginId, result)
      }
    }

    for ((problemClass, allProblemsOfClass) in allProblems.groupBy { it.javaClass }) {
      val problemTypeSuite = TeamCityResultPrinter.convertProblemClassNameToSentence(problemClass)
      tcLog.testSuiteStarted("($problemTypeSuite)").use {
        for ((shortDescription, problemsWithShortDescription) in allProblemsOfClass.groupBy { it.shortDescription }) {
          val testName = "($shortDescription)"
          tcLog.testStarted(testName).use {
            val plugin2Problems = ArrayListMultimap.create<PluginInfo, CompatibilityProblem>()
            for (problem in problemsWithShortDescription) {
              for (plugin in allProblem2Plugins.get(problem)) {
                plugin2Problems.put(plugin, problem)
              }
            }

            val testDetails = StringBuilder()
            for ((plugin, problems) in plugin2Problems.asMap()) {
              val latestPluginVerification = if (newTarget is PluginVerificationTarget.IDE) {
                newPluginIdToVerifications.get(plugin.pluginId).find {
                  it.plugin != plugin && it.plugin.isCompatibleWith(newTarget.ideVersion)
                }
              } else {
                null
              }

              val twoResults = pluginToTwoResults[plugin]
              val missingDependenciesNote = if (twoResults != null) {
                getMissingDependenciesNote(twoResults.oldResult, twoResults.newResult)
              } else {
                ""
              }

              val compatibilityProblems = buildString {
                for ((index, problem) in problems.withIndex()) {
                  if (problems.size > 1) {
                    append("${index + 1}) ")
                  }
                  appendln(problem.fullDescription)
                  if (latestPluginVerification != null) {
                    if (latestPluginVerification.isKnownProblem(problem)) {
                      appendln("This problem also takes place in the newest version of the plugin ${latestPluginVerification.plugin.getFullPluginCoordinates()}")
                    } else {
                      appendln("This problem does not take place in the newest version of the plugin ${latestPluginVerification.plugin.getFullPluginCoordinates()}")
                    }
                  }
                }
              }

              val compatibilityNote = createCompatibilityNote(plugin, baseTarget, newTarget, latestPluginVerification, problems)

              with(testDetails) {
                if (isNotEmpty()) {
                  appendln()
                  appendln()
                }

                appendln(plugin.getFullPluginCoordinates())
                appendln(compatibilityNote)
                if (missingDependenciesNote.isNotEmpty()) {
                  appendln()
                  appendln(missingDependenciesNote)
                  appendln()
                }
                appendln(compatibilityProblems)
              }
            }

            val testMessage = buildString {
              appendln(shortDescription)
              appendln("This problem is detected for $newTarget but not for $baseTarget.")
              appendln(documentationNote)
            }

            tcLog.testFailed(testName, testMessage, testDetails.toString())
          }
        }
      }
    }

    val newProblemsCnt = allProblems.distinctBy { it.shortDescription }.size
    val affectedPluginsCnt = allPlugin2Problems.keySet().size
    if (newProblemsCnt > 0) {
      tcLog.buildStatusFailure("$newProblemsCnt new " + "problem".pluralize(newProblemsCnt) + " detected in $newTarget compared to $baseTarget (affecting " + "plugin".pluralizeWithNumber(affectedPluginsCnt) + ")")
    } else {
      tcLog.buildStatusSuccess("No new compatibility problems found in $newTarget compared to $baseTarget")
    }
  }

  private val documentationNote: String
    get() = """
      If this incompatible change can't be reverted, it must be documented on 'Incompatible Changes in IntelliJ Platform and Plugins API Page'.
      If the problem is documented, it will be ignored by Plugin Verifier on the next verification. Note that TeamCity investigation may not disappear immediately.
      
      To document the change do the following:
      1) Open https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html
      2) Open a page corresponding to the current year, for example 'Changes in 2019.*'
      3) Click 'Edit Page' at the right upper corner to navigate to GitHub.
      4) Read tutorial on how to document breaking changes at the top, which starts with <!-- Before documenting a breaking API change ... --> 
      5) Add a documenting pattern (the first line) and the change reason (the second line starting with ':'). The pattern must be syntactically correct. See supported patterns at the top.
      6) Provide the commit message and open a pull request.
    """.trimIndent()

  private fun createCompatibilityNote(
      plugin: PluginInfo,
      baseTarget: PluginVerificationTarget,
      newTarget: PluginVerificationTarget,
      latestPluginVerification: PluginVerificationResult?,
      problems: Collection<CompatibilityProblem>
  ): String = buildString {
    if (baseTarget is PluginVerificationTarget.IDE
        && newTarget is PluginVerificationTarget.IDE
        && !plugin.isCompatibleWith(newTarget.ideVersion)
    ) {
      appendln(
          "Note that compatibility range ${plugin.presentableSinceUntilRange} " +
              "of plugin ${plugin.presentableName} does not include ${newTarget.ideVersion}."
      )
      if (latestPluginVerification != null) {
        appendln(
            "We have also verified the newest plugin version ${latestPluginVerification.plugin.presentableName} " +
                "whose compatibility range ${latestPluginVerification.plugin.presentableSinceUntilRange} includes ${newTarget.ideVersion}. "
        )
        val latestVersionSameProblemsCount = problems.count { latestPluginVerification.isKnownProblem(it) }
        if (latestVersionSameProblemsCount > 0) {
          appendln(
              "The newest version ${latestPluginVerification.plugin.version} has $latestVersionSameProblemsCount/${problems.size} same " + "problem".pluralize(latestVersionSameProblemsCount) + " " +
                  "and thus it has also been affected by this breaking change."
          )
        } else {
          appendln("The newest version ${latestPluginVerification.plugin.version} has none of the problems of the old version and thus it may be considered unaffected by this breaking change.")
        }
      } else {
        appendln("There are no newer versions of the plugin for ${newTarget.ideVersion}. ")
      }
    }
  }

  private fun PluginInfo.getFullPluginCoordinates(): String {
    val browserUrl = (this as? Browseable)?.browserUrl?.let { " $it" }.orEmpty()
    val updateId = (this as? UpdateInfo)?.updateId?.let { " (#$it)" }.orEmpty()
    return "$pluginId:$version$updateId$browserUrl"
  }

  private fun DependenciesGraph.getResolvedDependency(dependency: PluginDependency) =
      edges.find { it.dependency == dependency }?.to

  private fun PluginVerificationResult.getResolvedDependency(dependency: PluginDependency) =
      (this as? PluginVerificationResult.Verified)?.dependenciesGraph?.getResolvedDependency(dependency)

  private fun getMissingDependenciesNote(baseResult: PluginVerificationResult.Verified, newResult: PluginVerificationResult.Verified): String {
    val baseMissingDependencies = baseResult.directMissingDependencies
    val newMissingDependencies = newResult.directMissingDependencies
    if (newMissingDependencies.isEmpty()) {
      return ""
    }
    return buildString {
      appendln("Note: some problems might have been caused by missing dependencies: [")
      for ((dependency, missingReason) in newMissingDependencies) {
        append("    $dependency: $missingReason")

        val baseResolvedDependency = baseResult.getResolvedDependency(dependency)
        if (baseResolvedDependency != null) {
          append(" (when ${baseResult.verificationTarget} was checked, $baseResolvedDependency was used)")
        } else {
          val baseMissingDep = baseMissingDependencies.find { it.dependency == dependency }
          if (baseMissingDep != null) {
            append(" (it was also missing when we checked ${baseResult.verificationTarget} ")
            if (missingReason == baseMissingDep.missingReason) {
              append("by the same reason)")
            } else {
              append("by the following reason: ${baseMissingDep.missingReason})")
            }
          }
        }
        appendln()
      }
      appendln("]")
    }
  }

}

private fun TwoTargetsVerificationResults.getPluginToTwoResults(): Map<PluginInfo, TwoResults> {
  val basePlugin2Result = baseResults.associateBy { it.plugin }
  val newPlugin2Result = newResults.associateBy { it.plugin }

  val resultsComparisons = hashMapOf<PluginInfo, TwoResults>()

  for ((plugin, baseResult) in basePlugin2Result) {
    val newResult = newPlugin2Result[plugin]
    if (baseResult is PluginVerificationResult.Verified && newResult is PluginVerificationResult.Verified) {
      val newProblems = newResult.compatibilityProblems.filterNotTo(hashSetOf()) { baseResult.isKnownProblem(it) }
      resultsComparisons[plugin] = TwoResults(plugin, baseResult, newResult, newProblems)
    }
  }
  return resultsComparisons
}

private data class TwoResults(
    val plugin: PluginInfo,
    val oldResult: PluginVerificationResult.Verified,
    val newResult: PluginVerificationResult.Verified,
    val newProblems: Set<CompatibilityProblem>
)

/**
 * Determines whether the [problem] is known to this verification result.
 */
private fun PluginVerificationResult.isKnownProblem(problem: CompatibilityProblem): Boolean {
  val knownProblems = (this as? PluginVerificationResult.Verified)?.compatibilityProblems ?: return false
  if (problem in knownProblems) {
    return true
  }

  return when (problem) {
    is ClassNotFoundProblem -> {
      knownProblems.any { it is PackageNotFoundProblem && problem in it.classNotFoundProblems }
    }
    is PackageNotFoundProblem -> {
      problem.classNotFoundProblems.all { classNotFoundProblem ->
        isKnownProblem(classNotFoundProblem)
      }
    }
    is MethodNotFoundProblem -> {
      /*
      Problem "Method is not accessible" changed to "Method is not found":
      It is the case when, for example, the private method was removed.
      The plugins invoking the private method had been already broken, so deletion
      of the method doesn't lead to "new" API breakages.
      */
      knownProblems.any { it is IllegalMethodAccessProblem && it.bytecodeMethodReference == problem.unresolvedMethod }
    }
    is IllegalMethodAccessProblem -> {
      /*
      Problem "Method is not found" changed to "Method is not accessible":
      It is the case when, for example, the method was removed, and then re-added with weaker access modifier.
      The plugins invoking the missing method will fail with "Access Error",
      so this is not the "new" API breakage.
      */
      knownProblems.any { it is MethodNotFoundProblem && it.unresolvedMethod == problem.bytecodeMethodReference } ||
          knownProblems.any { it is IllegalMethodAccessProblem && it.bytecodeMethodReference == problem.bytecodeMethodReference }
    }
    is IllegalFieldAccessProblem -> {
      /*
      Problem "Field is not found" changed to "Field is not accessible":
      This is similar to the method's case.
       */
      knownProblems.any { it is FieldNotFoundProblem && it.unresolvedField == problem.fieldBytecodeReference } ||
          knownProblems.any { it is IllegalFieldAccessProblem && it.fieldBytecodeReference == problem.fieldBytecodeReference }
    }
    is FieldNotFoundProblem -> {
      /*
      Problem "Field is not accessible" changed to "Field is not found"
      This is similar to deletion of a inaccessible method.
       */
      knownProblems.any { it is IllegalFieldAccessProblem && it.fieldBytecodeReference == problem.unresolvedField }
    }
    else -> false
  }
}