package com.jetbrains.pluginverifier.tasks.common

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.html.HtmlResultPrinter
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.Browseable
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

class NewProblemsResultPrinter(
    private val outputOptions: OutputOptions,
    private val pluginRepository: PluginRepository
) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    with(taskResult as NewProblemsResult) {
      if (outputOptions.needTeamCityLog) {
        printResults(this)
      } else {
        println("Enable TeamCity results printing option (-team-city or -tc) to see the results in TeamCity builds format.")
      }

      HtmlResultPrinter(
          baseTarget,
          baseTarget.getReportDirectory(outputOptions.verificationReportsDirectory).resolve("report.html"),
          outputOptions.missingDependencyIgnoring
      ).printResults(baseResults)

      HtmlResultPrinter(
          newTarget,
          newTarget.getReportDirectory(outputOptions.verificationReportsDirectory).resolve("report.html"),
          outputOptions.missingDependencyIgnoring
      ).printResults(newResults)
    }
  }

  private fun NewProblemsResult.getNewPluginProblems(): Multimap<PluginInfo, CompatibilityProblem> {
    val result = HashMultimap.create<PluginInfo, CompatibilityProblem>()
    for ((plugin, cmp) in resultsComparisons) {
      result.putAll(plugin, cmp.newProblems)
    }
    return result
  }

  private fun PluginInfo.presentableSinceUntilRange(): String? {
    if (sinceBuild != null) {
      if (untilBuild != null) {
        return "[$sinceBuild; $untilBuild]"
      }
      return "$sinceBuild+"
    }
    return null
  }

  private fun StringBuilder.appendIdeCompatibilityNote(
      plugin: PluginInfo,
      baseTarget: VerificationTarget,
      newTarget: VerificationTarget
  ) {
    if (baseTarget is VerificationTarget.Ide && newTarget is VerificationTarget.Ide && !plugin.isCompatibleWith(newTarget.ideVersion)) {
      val baseIdeVersion = baseTarget.ideVersion
      val newIdeVersion = newTarget.ideVersion

      val sinceUntil = plugin.presentableSinceUntilRange()
      append("Note that the compatibility range ")
      if (sinceUntil != null) {
        append("$sinceUntil ")
      }
      append("of $plugin doesn't include $newIdeVersion. ")

      val lastCompatibleVersion = try {
        pluginRepository.getLastCompatibleVersionOfPlugin(newIdeVersion, plugin.pluginId)
      } catch (ie: InterruptedException) {
        throw ie
      } catch (e: Exception) {
        null
      }
      checkIfInterrupted()

      if (lastCompatibleVersion != null) {
        append(
            "There is a newer version '${lastCompatibleVersion.version}' compatible with $newIdeVersion " +
                "in the Plugin Repository. Anyway the ${plugin.version} has been checked because this check configuration " +
                "tracks breaking API changes of the IntelliJ Platform between $baseIdeVersion and $newIdeVersion, " +
                "not the plugins' incompatibilities."
        )
      } else {
        append(
            "Though the '${plugin.version}' cannot be installed in $newIdeVersion, " +
                "incompatible API changes are discouraged and should be avoided as stated in " +
                "the API compatibility policy: https://confluence.jetbrains.com/display/IDEA/IntelliJ+Platform+API+compatibility+policy"
        )
      }
      appendln()
    }
  }

  private fun printResults(newProblemsResult: NewProblemsResult) {
    val tcLog = TeamCityLog(System.out)

    val allPlugin2Problems = newProblemsResult.getNewPluginProblems()
    val allProblem2Plugins = Multimaps.invertFrom(allPlugin2Problems, HashMultimap.create<CompatibilityProblem, PluginInfo>())
    val allProblems = allProblem2Plugins.keySet()

    val baseTarget = newProblemsResult.baseTarget
    val newTarget = newProblemsResult.newTarget

    for ((problemClass, allProblemsOfClass) in allProblems.groupBy { it.javaClass }) {
      val problemTypeSuite = TeamCityResultPrinter.convertProblemClassNameToSentence(problemClass)
      tcLog.testSuiteStarted("($problemTypeSuite)").use {
        for ((shortDescription, problemsWithShortDescription) in allProblemsOfClass.groupBy { it.shortDescription }) {
          val testName = "($shortDescription)"
          tcLog.testStarted(testName).use {
            val testMessage = buildString {
              appendln(shortDescription)
              appendln("This problem is detected for $newTarget but not for $baseTarget.")
            }

            val plugin2Problems = ArrayListMultimap.create<PluginInfo, CompatibilityProblem>()
            for (problem in problemsWithShortDescription) {
              for (plugin in allProblem2Plugins.get(problem)) {
                plugin2Problems.put(plugin, problem)
              }
            }

            val testDetails = buildString {
              for ((plugin, problems) in plugin2Problems.asMap()) {
                val urlSuffix = (plugin as? Browseable)?.browserUrl?.let { " $it" }.orEmpty()
                val updateIdSuffix = (plugin as? UpdateInfo)?.updateId?.let { " ($it)" }.orEmpty()
                appendln()
                appendln(plugin.pluginId + ":" + plugin.version + urlSuffix + updateIdSuffix)
                appendIdeCompatibilityNote(plugin, baseTarget, newTarget)
                appendMissingDependenciesNotes(newProblemsResult, plugin)
                appendln()

                for ((index, problem) in problems.withIndex()) {
                  if (problems.size > 1) {
                    append("${index + 1}) ")
                  }
                  appendln(problem.fullDescription)
                }
              }
            }
            tcLog.testFailed(testName, testMessage, testDetails)
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

  private fun DependenciesGraph.getResolvedDependency(dependency: PluginDependency) =
      edges.find { it.dependency == dependency }?.to

  private fun VerificationResult.getResolvedDependency(dependency: PluginDependency) =
      dependenciesGraph.getResolvedDependency(dependency)

  private fun StringBuilder.appendMissingDependenciesNotes(newProblemsResult: NewProblemsResult, plugin: PluginInfo) {
    val comparison = newProblemsResult.resultsComparisons[plugin] ?: return

    val baseResult = comparison.oldResult
    val baseMissingDependencies = comparison.oldDirectMissingDependencies
    val newMissingDependencies = comparison.newDirectMissingDependencies

    if (newMissingDependencies.isNotEmpty()) {
      appendln("Note: some problems might have been caused by missing dependencies: [")
      for ((dependency, missingReason) in newMissingDependencies) {
        append("    $dependency: $missingReason")

        val baseResolvedDependency = baseResult.getResolvedDependency(dependency)
        if (baseResolvedDependency != null) {
          append(" (when ${newProblemsResult.baseTarget} was checked, $baseResolvedDependency was used)")
        } else {
          val baseMissingDep = baseMissingDependencies.find { it.dependency == dependency }
          if (baseMissingDep != null) {
            append(" (it was also missing when we checked ${newProblemsResult.baseTarget} ")
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