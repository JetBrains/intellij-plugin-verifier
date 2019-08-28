package com.jetbrains.pluginverifier.output.teamcity

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.output.ResultPrinter
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
) : ResultPrinter {

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

  fun printNoCompatibleVersionsProblems(missingVersionsProblems: List<MissingCompatibleVersionProblem>) {
    return when (groupBy) {
      GroupBy.NOT_GROUPED -> {
        missingVersionsProblems.forEach { tcLog.buildProblem(it.toString()) }
      }
      GroupBy.BY_PLUGIN -> {
        missingVersionsProblems.forEach { missingProblem ->
          tcLog.testSuiteStarted(missingProblem.pluginId).use {
            val testName = "(no compatible version)"
            tcLog.testStarted(testName).use {
              tcLog.testFailed(testName, "#$missingProblem\n", "")
            }
          }
        }
      }
      GroupBy.BY_PROBLEM_TYPE -> {
        tcLog.testSuiteStarted("(no compatible version)").use {
          missingVersionsProblems.forEach { problem ->
            tcLog.testSuiteStarted(problem.pluginId).use {
              val testName = problem.pluginId
              tcLog.testStarted(testName).use {
                tcLog.testFailed(testName, "#$problem\n", "")
              }
            }
          }
        }
      }
    }

  }


  override fun printResults(results: List<PluginVerificationResult>) {
    when (groupBy) {
      GroupBy.NOT_GROUPED -> notGrouped(results)
      GroupBy.BY_PROBLEM_TYPE -> groupByProblemType(results)
      GroupBy.BY_PLUGIN -> groupByPlugin(results)
    }
  }

  private fun notGrouped(results: List<PluginVerificationResult>) {
    //problem1 (in a:1.0, a:1.2, b:1.0)
    //problem2 (in a:1.0, c:1.3)
    //missing dependencies: missing#1 (required for plugin1, plugin2, plugin3)
    //missing dependencies: missing#2 (required for plugin2, plugin4)

    printProblemAndAffectedPluginsAsBuildProblem(results)
    printMissingDependenciesAndRequiredPluginsAsBuildProblem(results)
  }

  private fun printProblemAndAffectedPluginsAsBuildProblem(results: List<PluginVerificationResult>) {
    val shortDescription2Plugins: Multimap<String, PluginInfo> = HashMultimap.create()
    for (result in results) {
      for (problem in result.getProblems()) {
        shortDescription2Plugins.put(problem.shortDescription, result.plugin)
      }
    }
    shortDescription2Plugins.asMap().forEach { (description, allPluginsWithThisProblem) ->
      tcLog.buildProblem("$description (in ${allPluginsWithThisProblem.joinToString()})")
    }
  }

  private fun PluginVerificationResult.getProblems(): Set<CompatibilityProblem> =
      if (this is PluginVerificationResult.Verified) {
        compatibilityProblems
      } else {
        emptySet()
      }

  private fun printMissingDependenciesAndRequiredPluginsAsBuildProblem(results: List<PluginVerificationResult>) {
    val missingToRequired = collectMissingDependenciesForRequiringPlugins(results)
    missingToRequired.asMap().entries.forEach {
      tcLog.buildProblem("Missing dependency ${it.key} (required for ${it.value.joinToString()})")
    }
  }

  private fun collectMissingDependenciesForRequiringPlugins(results: List<PluginVerificationResult>): Multimap<MissingDependency, PluginInfo> {
    val missingToRequiring = HashMultimap.create<MissingDependency, PluginInfo>()
    results.filterIsInstance<PluginVerificationResult.Verified>().forEach {
      it.directMissingDependencies.forEach { missingDependency ->
        missingToRequiring.put(missingDependency, it.plugin)
      }
    }
    return missingToRequiring
  }


  private fun groupByPlugin(results: List<PluginVerificationResult>) {
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
    val verificationTargets = results.map { it.verificationTarget }.distinct()
    val targetToLastPluginVersions = requestLastVersionsOfCheckedPlugins(verificationTargets)
    results.groupBy { it.plugin.pluginId }.forEach { (pluginId, pluginResults) ->
      printResultsForSpecificPluginId(pluginId, pluginResults, targetToLastPluginVersions)
    }
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
  ) {
    tcLog.testSuiteStarted(pluginId).use {
      pluginResults.groupBy { it.plugin.version }.forEach { versionToResults ->
        versionToResults.value.forEach { result ->
          val testName = getPluginVersionAsTestName(result.plugin, result.verificationTarget, targetToLastPluginVersions)
          printResultOfSpecificVersion(result.plugin, result, testName)
        }
      }
    }
  }

  private fun printResultOfSpecificVersion(
      plugin: PluginInfo,
      verificationResult: PluginVerificationResult,
      testName: String
  ) {
    tcLog.testStarted(testName).use {
      return@use when (verificationResult) {
        is PluginVerificationResult.Verified -> when {
          verificationResult.hasCompatibilityProblems -> printCompatibilityProblemsAndMissingDependencies(plugin, testName, verificationResult.compatibilityProblems, emptyList())
          verificationResult.hasDirectMissingDependencies -> printCompatibilityProblemsAndMissingDependencies(plugin, testName, verificationResult.compatibilityProblems, verificationResult.directMissingDependencies)
          else -> Unit
        }
        is PluginVerificationResult.InvalidPlugin -> printBadPluginResult(verificationResult, testName)
        else -> Unit
      }
    }
  }

  private fun printCompatibilityProblemsAndMissingDependencies(
      plugin: PluginInfo,
      testName: String,
      problems: Set<CompatibilityProblem>,
      missingDependencies: List<MissingDependency>
  ) {
    val mandatoryMissingDependencies = missingDependencies.filterNot { it.dependency.isOptional }
    if (problems.isNotEmpty() || mandatoryMissingDependencies.isNotEmpty()) {
      val overview = buildString {
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
      }

      val notFoundClassesProblems = problems.filterIsInstance<ClassNotFoundProblem>()
      val problemsContent = if (missingDependencies.isNotEmpty() && notFoundClassesProblems.size > 20) {
        getTooManyUnknownClassesProblems(notFoundClassesProblems, problems)
      } else {
        getProblemsContent(problems)
      }

      tcLog.testStdErr(testName, overview)
      tcLog.testFailed(testName, problemsContent, "")
    }
  }

  private fun getPluginOverviewLink(plugin: PluginInfo): String {
    val url = (plugin as? Browseable)?.browserUrl ?: return ""
    return "Plugin URL: $url"
  }

  private fun printBadPluginResult(verificationResult: PluginVerificationResult.InvalidPlugin, versionTestName: String) {
    val message = "Plugin is invalid: ${verificationResult.pluginStructureErrors.joinToString()}"
    tcLog.testStdErr(versionTestName, message)
    tcLog.testFailed(versionTestName, message, "")
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
          emptyList<PluginInfo>() //Kotlin fails to determine type.
        }
      }

  private fun requestLastVersionsOfEachCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> {
    val plugins = repository.getLastCompatiblePlugins(ideVersion)
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

  private fun groupByProblemType(results: List<PluginVerificationResult>) {
    //accessing to unknown class SomeClass
    //....(pluginOne:1.2.0)
    //....(pluginTwo:2.0.0)
    //invoking unknown method method
    //....(pluginThree:1.0.0)
    //missing dependencies
    //....(missing#1)
    //.........Required for plugin1, plugin2, plugin3
    val problem2Plugin: Multimap<CompatibilityProblem, PluginInfo> = HashMultimap.create()
    for (result in results) {
      for (problem in result.getProblems()) {
        problem2Plugin.put(problem, result.plugin)
      }
    }

    val allProblems = problem2Plugin.keySet()
    for ((problemClass, problemsOfClass) in allProblems.groupBy { it.javaClass }) {
      val prefix = convertProblemClassNameToSentence(problemClass)
      tcLog.testSuiteStarted("($prefix)").use {
        for (problem in problemsOfClass) {
          for (plugin in problem2Plugin.get(problem)) {
            tcLog.testSuiteStarted(problem.shortDescription).use {
              val testName = "($plugin)"
              tcLog.testStarted(testName).use {
                tcLog.testFailed(testName, getPluginOverviewLink(plugin) + "\nPlugin: $plugin", problem.fullDescription)
              }
            }
          }
        }
      }
    }

    printMissingDependenciesAsTests(results)
  }

  private fun printMissingDependenciesAsTests(results: List<PluginVerificationResult>) {
    val missingToRequired = collectMissingDependenciesForRequiringPlugins(results)
    if (!missingToRequired.isEmpty) {
      tcLog.testSuiteStarted("(missing dependencies)").use {
        missingToRequired.asMap().entries.forEach { entry ->
          val testName = "(${entry.key})"
          tcLog.testStarted(testName).use {
            tcLog.testFailed(testName, "Required for ${entry.value.joinToString()}", "")
          }
        }
      }
    }
  }


  enum class GroupBy(private val arg: String) {
    NOT_GROUPED("not-grouped"),
    BY_PROBLEM_TYPE("problem_type"),
    BY_PLUGIN("plugin");

    companion object {

      @JvmStatic
      fun parse(groupValue: String?): GroupBy {
        groupValue ?: return NOT_GROUPED
        return values().find { it.arg == groupValue } ?: NOT_GROUPED
      }
    }
  }

}