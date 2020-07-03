/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.teamcity

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.repository.Browseable
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.checkIde.MissingCompatibleVersionProblem
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TeamCityResultPrinter(
  private val tcLog: TeamCityLog,
  private val groupBy: GroupBy,
  private val repository: PluginRepository
) {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(TeamCityResultPrinter::class.java)

    /**
     * Converts string like "com.some.package.name.MyClassNameProblem" to "my class name"
     */
    fun convertProblemClassNameToSentence(clazz: Class<CompatibilityProblem>): String {
      val name = clazz.name.substringAfterLast(".")
      var words = name.split("(?=[A-Z])".toRegex()).dropWhile { it.isEmpty() }
      if (words.isEmpty()) {
        return name.toLowerCase()
      }
      if (words.last() == "Problem") {
        words = words.dropLast(1)
      }
      return words.joinToString(" ") { it.toLowerCase() }
    }

    fun printInvalidPluginFiles(tcLog: TeamCityLog, invalidPluginFiles: List<InvalidPluginFile>) {
      if (invalidPluginFiles.isNotEmpty()) {
        val testName = "(invalid plugins)"
        tcLog.testStarted(testName).use {
          val message = buildString {
            for ((pluginFile, pluginErrors) in invalidPluginFiles) {
              append(pluginFile)
              for (pluginError in pluginErrors) {
                append("    $pluginError")
              }
            }
          }
          tcLog.testFailed(testName, message, "")
        }
      }
    }

  }

  fun printNoCompatibleVersionsProblems(missingVersionsProblems: List<MissingCompatibleVersionProblem>): TeamCityHistory {
    val failedTests = arrayListOf<TeamCityTest>()
    when (groupBy) {
      GroupBy.BY_PLUGIN -> {
        missingVersionsProblems.forEach { missingProblem ->
          val testSuiteName = missingProblem.pluginId
          tcLog.testSuiteStarted(testSuiteName).use {
            val testName = "(no compatible version)"
            tcLog.testStarted(testName).use {
              failedTests += TeamCityTest(testSuiteName, testName)
              tcLog.testFailed(testName, "#$missingProblem\n", "")
            }
          }
        }
      }
      GroupBy.BY_PROBLEM_TYPE -> {
        val testSuiteName = "(no compatible version)"
        tcLog.testSuiteStarted(testSuiteName).use {
          missingVersionsProblems.forEach { problem ->
            tcLog.testSuiteStarted(problem.pluginId).use {
              val testName = problem.pluginId
              tcLog.testStarted(testName).use {
                failedTests += TeamCityTest(testSuiteName, testName)
                tcLog.testFailed(testName, "#$problem\n", "")
              }
            }
          }
        }
      }
    }
    return TeamCityHistory(failedTests)
  }


  fun printResults(results: List<PluginVerificationResult>): TeamCityHistory =
    when (groupBy) {
      GroupBy.BY_PROBLEM_TYPE -> groupByProblemType(results)
      GroupBy.BY_PLUGIN -> groupByPlugin(results)
    }

  private fun PluginVerificationResult.getProblems(): Set<CompatibilityProblem> =
    if (this is PluginVerificationResult.Verified) {
      compatibilityProblems
    } else {
      emptySet()
    }

  private fun collectMissingDependenciesForRequiringPlugins(results: List<PluginVerificationResult>): Map<MissingDependency, Set<PluginInfo>> {
    val missingToRequiring = mutableMapOf<MissingDependency, MutableSet<PluginInfo>>()
    results.filterIsInstance<PluginVerificationResult.Verified>().forEach {
      it.directMissingMandatoryDependencies.forEach { missingDependency ->
        missingToRequiring.getOrPut(missingDependency) { hashSetOf() } += it.plugin
      }
    }
    return missingToRequiring
  }


  //pluginOne
  //....(1.0)
  //........#invoking unknown method
  //............someClass
  //........#accessing to unknown class
  //............another class
  //....(1.2)
  //........#invoking unknown method
  //............someClass
  //........missing non-optional dependency dep#1
  //........missing non-optional dependency pluginOne:1.2 -> otherPlugin:3.3 -> dep#2 (it doesn't have a compatible build with IDE #IU-162.1121.10)
  //........missing optional dependency dep#3
  //pluginTwo
  //...and so on...
  private fun groupByPlugin(results: List<PluginVerificationResult>): TeamCityHistory {
    val failedTests = arrayListOf<TeamCityTest>()
    val verificationTargets = results.map { it.verificationTarget }.distinct()
    val targetToLastPluginVersions = requestLastVersionsOfCheckedPlugins(verificationTargets)
    results.groupBy { it.plugin.pluginId }.forEach { (pluginId, pluginResults) ->
      failedTests += printResultsForSpecificPluginId(pluginId, pluginResults, targetToLastPluginVersions)
    }
    return TeamCityHistory(failedTests)
  }

  /**
   * Generates the test group with name equal to the [pluginId]
   * and for each verified version of the plugin
   * creates a separate test. Thus the layout is as follows:
   *
   * ```
   * plugin.id.one <- test group equal to [pluginId]
   * ....(1.0)     <- test name equal to the plugin's version
   * ........results of the plugin.id.one:1.0
   * ....(2.0)
   * ........results of the plugin.id.one:2.0
   * plugin.id.two
   * ....(1.5)
   * ........results of the plugin id.two:1.5
   * ....and so on...
   * ```
   */
  private fun printResultsForSpecificPluginId(
    pluginId: String,
    pluginResults: List<PluginVerificationResult>,
    targetToLastPluginVersions: Map<PluginVerificationTarget, List<PluginInfo>>
  ): List<TeamCityTest> {
    val failedTests = arrayListOf<TeamCityTest>()
    tcLog.testSuiteStarted(pluginId).use {
      pluginResults.groupBy { it.plugin.version }.forEach { versionToResults ->
        versionToResults.value.forEach { result ->
          val testName = getPluginVersionAsTestName(result.plugin, result.verificationTarget, targetToLastPluginVersions)
          tcLog.testStarted(testName).use {
            when (result) {
              is PluginVerificationResult.Verified -> {
                val message = getMessageCompatibilityProblemsAndMissingDependencies(result.plugin, result.compatibilityProblems, result.directMissingMandatoryDependencies)
                if (message != null) {
                  failedTests += TeamCityTest(pluginId, testName)
                  tcLog.testFailed(testName, message, "")
                }
              }
              is PluginVerificationResult.InvalidPlugin -> {
                val message = "Plugin is invalid: ${result.pluginStructureErrors.joinToString()}"
                failedTests += TeamCityTest(pluginId, testName)
                tcLog.testFailed(testName, message, "")
                Unit
              }
              else -> Unit
            }
          }
        }
      }
    }
    return failedTests
  }

  private fun getMessageCompatibilityProblemsAndMissingDependencies(
    plugin: PluginInfo,
    problems: Set<CompatibilityProblem>,
    missingDependencies: List<MissingDependency>
  ): String? {
    val mandatoryMissingDependencies = missingDependencies.filterNot { it.dependency.isOptional }
    if (problems.isNotEmpty() || mandatoryMissingDependencies.isNotEmpty()) {
      return buildString {
        appendln(getPluginOverviewLink(plugin))
        if (problems.isNotEmpty()) {
          appendln("$plugin has ${problems.size} compatibility " + "problem".pluralize(problems.size))
        }

        if (missingDependencies.isNotEmpty()) {
          if (problems.isNotEmpty()) {
            appendln("Some problems might have been caused by missing dependencies: ")
          }
          for (missingDependency in missingDependencies) {
            appendln("Missing dependency ${missingDependency.dependency}: ${missingDependency.missingReason}")
          }
        }

        val notFoundClassesProblems = problems.filterIsInstance<ClassNotFoundProblem>()
        val problemsContent = if (missingDependencies.isNotEmpty() && notFoundClassesProblems.size > 20) {
          getTooManyUnknownClassesProblems(notFoundClassesProblems, problems)
        } else {
          getProblemsContent(problems)
        }

        appendln()
        appendln(problemsContent)
      }
    }
    return null
  }

  private fun getPluginOverviewLink(plugin: PluginInfo): String {
    val url = (plugin as? Browseable)?.browserUrl ?: return ""
    return "Plugin URL: $url"
  }

  private fun getProblemsContent(problems: Iterable<CompatibilityProblem>): String = buildString {
    for ((shortDescription, problemsWithShortDescription) in problems.groupBy { it.shortDescription }) {
      appendln("#$shortDescription")
      for (compatibilityProblem in problemsWithShortDescription) {
        appendln("    ${compatibilityProblem.fullDescription}")
      }
    }
  }

  private fun getTooManyUnknownClassesProblems(
    notFoundClassesProblems: List<ClassNotFoundProblem>,
    problems: Set<CompatibilityProblem>
  ): String {
    val otherProblems = getProblemsContent(problems.filterNot { it in notFoundClassesProblems })
    return buildString {
      appendln("There are too many missing classes (${notFoundClassesProblems.size});")
      appendln("it's probably because of missing plugins or modules")
      appendln("some not-found classes: [${notFoundClassesProblems.take(20).map { it.unresolved }.joinToString()}...];")
      if (otherProblems.isNotEmpty()) {
        appendln("Other problems: ")
        appendln(otherProblems)
      }
    }
  }

  /**
   * For each IDE returns the last versions f the plugin available in the [repository]
   * and compatible with this IDE.
   */
  private fun requestLastVersionsOfCheckedPlugins(verificationTargets: List<PluginVerificationTarget>): Map<PluginVerificationTarget, List<PluginInfo>> =
    verificationTargets.associateWith { target ->
      try {
        when (target) {
          is PluginVerificationTarget.IDE -> {
            requestLastVersionsOfEachCompatiblePlugins(target.ideVersion)
          }
          is PluginVerificationTarget.Plugin -> emptyList()
        }
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        LOG.info("Unable to determine the last compatible updates of IDE $target", e)
        @Suppress("RemoveExplicitTypeArguments")
        emptyList<PluginInfo>()
      }
    }

  private fun requestLastVersionsOfEachCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> {
    val plugins = runCatching { repository.getLastCompatiblePlugins(ideVersion) }.getOrDefault(emptyList())
    return plugins.groupBy { it.pluginId }.mapValues { (_, sameIdPlugins) ->
      if (repository is MarketplaceRepository) {
        sameIdPlugins.maxBy { (it as UpdateInfo).updateId }
      } else {
        sameIdPlugins.maxWith(compareBy(VersionComparatorUtil.COMPARATOR) { it.version })
      }
    }.values.filterNotNull()
  }

  /**
   * Generates a TC test name in which the verification report will be printed.
   *
   * The test name is the version of the plugin
   * plus, possibly, the suffix 'newest' indicating that this
   * is the last available version of the plugin.
   *
   * The test name is wrapped into parenthesis like so `(<version>)`
   * to make TC display the version as a whole. Not doing this
   * would lead to TC arbitrarily splitting the version.
   *
   * Examples are:
   * 1) `(173.3727.144.8)`
   * 2) `(173.3727.244.997 - newest)`
   */
  private fun getPluginVersionAsTestName(
    pluginInfo: PluginInfo,
    verificationTarget: PluginVerificationTarget,
    ideLastPluginVersions: Map<PluginVerificationTarget, List<PluginInfo>>
  ): String {
    val lastVersions = ideLastPluginVersions.getOrDefault(verificationTarget, emptyList())
    return if (pluginInfo in lastVersions) {
      "(${pluginInfo.version} - newest)"
    } else {
      "(${pluginInfo.version})"
    }
  }

  //accessing to unknown class SomeClass
  //....(pluginOne:1.2.0)
  //....(pluginTwo:2.0.0)
  //invoking unknown method method
  //....(pluginThree:1.0.0)
  //missing dependencies
  //....(missing#1)
  //.........Required for plugin1, plugin2, plugin3
  private fun groupByProblemType(results: List<PluginVerificationResult>): TeamCityHistory {
    val failedTests = arrayListOf<TeamCityTest>()

    val problem2Plugin: MutableMap<CompatibilityProblem, MutableSet<PluginInfo>> = hashMapOf()
    for (result in results) {
      for (problem in result.getProblems()) {
        problem2Plugin.getOrPut(problem) { hashSetOf() } += result.plugin
      }
    }

    val allProblems = problem2Plugin.keys
    for ((problemClass, problemsOfClass) in allProblems.groupBy { it.javaClass }) {
      val prefix = convertProblemClassNameToSentence(problemClass)
      val testSuiteName = "($prefix)"
      tcLog.testSuiteStarted(testSuiteName).use {
        for (problem in problemsOfClass) {
          for (plugin in (problem2Plugin[problem] ?: emptySet<PluginInfo>())) {
            tcLog.testSuiteStarted(problem.shortDescription).use {
              val testName = "($plugin)"
              tcLog.testStarted(testName).use {
                failedTests += TeamCityTest(testSuiteName, testName)
                tcLog.testFailed(testName, getPluginOverviewLink(plugin) + "\nPlugin: $plugin", problem.fullDescription)
              }
            }
          }
        }
      }
    }

    val missingToRequired = collectMissingDependenciesForRequiringPlugins(results)
    if (missingToRequired.isNotEmpty()) {
      val testSuiteName = "(missing dependencies)"
      tcLog.testSuiteStarted(testSuiteName).use {
        missingToRequired.entries.forEach { (key, values) ->
          val testName = "($key)"
          tcLog.testStarted(testName).use {
            failedTests += TeamCityTest(testSuiteName, testName)
            tcLog.testFailed(testName, "Required for ${values.joinToString()}", "")
          }
        }
      }
    }

    return TeamCityHistory(failedTests)
  }


  enum class GroupBy(private val arg: String) {
    BY_PROBLEM_TYPE("problem_type"),
    BY_PLUGIN("plugin");

    companion object {

      @JvmStatic
      fun parse(groupValue: String?): GroupBy =
        values().find { it.arg == groupValue } ?: BY_PLUGIN
    }
  }

}