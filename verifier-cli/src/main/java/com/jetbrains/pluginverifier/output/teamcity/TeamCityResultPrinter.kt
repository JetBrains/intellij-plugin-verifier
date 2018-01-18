package com.jetbrains.pluginverifier.output.teamcity

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.output.settings.dependencies.MissingDependencyIgnoring
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.checkIde.MissingCompatibleUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TeamCityResultPrinter(private val tcLog: TeamCityLog,
                            private val groupBy: GroupBy,
                            private val repository: PluginRepository,
                            private val missingDependencyIgnoring: MissingDependencyIgnoring) : ResultPrinter {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(TeamCityResultPrinter::class.java)

    /**
     * Converts string like "com.some.package.name.MyClassNameProblem" to "my class name"
     */
    fun convertProblemClassNameToSentence(clazz: Class<CompatibilityProblem>): String {
      val name = clazz.name.substringAfterLast(".")
      var words = name.split("(?=[A-Z])".toRegex())
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

  fun printNoCompatibleUpdatesProblems(missingProblems: List<MissingCompatibleUpdate>) {
    return when (groupBy) {
      GroupBy.NOT_GROUPED -> {
        missingProblems.forEach { tcLog.buildProblem(it.toString()) }
      }
      GroupBy.BY_PLUGIN -> {
        missingProblems.forEach { missingProblem ->
          tcLog.testSuiteStarted(missingProblem.pluginId).use {
            val testName = "(no compatible update)"
            tcLog.testStarted(testName).use {
              tcLog.testFailed(testName, "#$missingProblem\n", "")
            }
          }
        }
      }
      GroupBy.BY_PROBLEM_TYPE -> {
        tcLog.testSuiteStarted("(no compatible update)").use {
          missingProblems.forEach { problem ->
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


  override fun printResults(results: List<VerificationResult>) {
    when (groupBy) {
      GroupBy.NOT_GROUPED -> notGrouped(results)
      GroupBy.BY_PROBLEM_TYPE -> groupByProblemType(results)
      GroupBy.BY_PLUGIN -> groupByPlugin(results)
    }
  }

  private fun notGrouped(results: List<VerificationResult>) {
    //problem1 (in a:1.0, a:1.2, b:1.0)
    //problem2 (in a:1.0, c:1.3)
    //missing dependencies: missing#1 (required for plugin1, plugin2, plugin3)
    //missing dependencies: missing#2 (required for plugin2, plugin4)

    printProblemAndAffectedPluginsAsBuildProblem(results)
    printMissingDependenciesAndRequiredPluginsAsBuildProblem(results)
  }

  private fun printProblemAndAffectedPluginsAsBuildProblem(results: List<VerificationResult>) {
    val shortDescription2Plugins: Multimap<String, PluginInfo> = HashMultimap.create()
    for (result in results) {
      for (problem in result.getProblems()) {
        shortDescription2Plugins.put(problem.shortDescription, result.plugin)
      }
    }
    shortDescription2Plugins.asMap().forEach { description, allPluginsWithThisProblem ->
      tcLog.buildProblem("$description (in ${allPluginsWithThisProblem.joinToString()})")
    }
  }

  private fun VerificationResult.getProblems() = when (this) {
    is VerificationResult.CompatibilityProblems -> problems
    is VerificationResult.MissingDependencies -> problems
    is VerificationResult.OK,
    is VerificationResult.StructureWarnings,
    is VerificationResult.InvalidPlugin,
    is VerificationResult.NotFound,
    is VerificationResult.FailedToDownload -> emptySet()
  }

  private fun printMissingDependenciesAndRequiredPluginsAsBuildProblem(results: List<VerificationResult>) {
    val missingToRequired = collectMissingDependenciesForRequiringPlugins(results)
    missingToRequired.asMap().entries.forEach {
      tcLog.buildProblem("Missing dependency ${it.key} (required for ${it.value.joinToString()})")
    }
  }

  private fun collectMissingDependenciesForRequiringPlugins(results: List<VerificationResult>): Multimap<MissingDependency, PluginInfo> {
    val missingToRequiring = HashMultimap.create<MissingDependency, PluginInfo>()
    results.filter { it is VerificationResult.MissingDependencies }.forEach {
      (it as VerificationResult.MissingDependencies).directMissingDependencies.forEach { missingDependency ->
        missingToRequiring.put(missingDependency, it.plugin)
      }
    }
    return missingToRequiring
  }


  private fun groupByPlugin(results: List<VerificationResult>) {
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
    val ideLastPluginVersions = requestLastVersionsOfCheckedPlugins(results.map { it.ideVersion }.distinct())
    results.groupBy { it.plugin.pluginId }.forEach { (pluginId, pluginResults) ->
      printResultsForSpecificPluginId(pluginId, pluginResults, ideLastPluginVersions)
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
  private fun printResultsForSpecificPluginId(pluginId: String,
                                              pluginResults: List<VerificationResult>,
                                              ideLastPluginVersions: Map<IdeVersion, List<PluginInfo>>) {
    tcLog.testSuiteStarted(pluginId).use {
      pluginResults.groupBy { it.plugin.version }.forEach { versionToResults ->
        versionToResults.value.forEach { result ->
          val testName = getPluginVersionAsTestName(result.plugin, result.ideVersion, ideLastPluginVersions)
          printResultOfSpecificVersion(result.plugin, result, testName)
        }
      }
    }
  }

  private fun printResultOfSpecificVersion(plugin: PluginInfo,
                                           verificationResult: VerificationResult,
                                           testName: String) {
    tcLog.testStarted(testName).use {
      return@use when (verificationResult) {
        is VerificationResult.CompatibilityProblems -> printProblems(plugin, testName, verificationResult.problems)
        is VerificationResult.MissingDependencies -> printMissingDependencies(plugin, verificationResult, testName)
        is VerificationResult.InvalidPlugin -> printBadPluginResult(verificationResult, testName)
        is VerificationResult.OK,
        is VerificationResult.StructureWarnings,
        is VerificationResult.NotFound,
        is VerificationResult.FailedToDownload -> {
        }
      }
    }
  }

  private fun printMissingDependencies(plugin: PluginInfo,
                                       verificationResult: VerificationResult.MissingDependencies,
                                       testName: String) {
    val problems = verificationResult.problems
    val missingDependencies = verificationResult.directMissingDependencies
    if (problems.isNotEmpty() || missingDependencies.any { !it.dependency.isOptional }) {
      val overview = buildString {
        append(getPluginOverviewLink(plugin)).append("\n")
        if (problems.isNotEmpty()) {
          append("$plugin has ${"problem".pluralizeWithNumber(problems.size)}\n")
        }
        if (missingDependencies.isNotEmpty()) {
          if (problems.isNotEmpty()) {
            append("Some problems might have been caused by missing plugins:").append('\n')
          }
          appendMissingDependencies(missingDependencies.filterNot { it.dependency.isOptional })
          appendMissingDependencies(missingDependencies.filter { it.dependency.isOptional && !missingDependencyIgnoring.ignoreMissingOptionalDependency(it.dependency) })
        }
      }
      val problemsContent = getMissingDependenciesProblemsContent(verificationResult)
      tcLog.testStdErr(testName, overview)
      tcLog.testFailed(testName, problemsContent, "")
    }
  }

  private fun getPluginOverviewLink(plugin: PluginInfo): String {
    val url = (plugin as? UpdateInfo)?.browserURL ?: return ""
    return "Plugin URL: $url"
  }

  private fun printBadPluginResult(verificationResult: VerificationResult.InvalidPlugin, versionTestName: String) {
    val message = "Plugin is invalid: ${verificationResult.pluginStructureErrors.joinToString()}"
    tcLog.testStdErr(versionTestName, message)
    tcLog.testFailed(versionTestName, message, "")
  }

  private fun printProblems(plugin: PluginInfo,
                            testName: String,
                            problems: Set<CompatibilityProblem>) {
    val overview = getPluginOverviewLink(plugin) + "\n$plugin has ${problems.size} ${"problem".pluralize(problems.size)}\n"
    val problemsContent = getProblemsContent(problems)
    tcLog.testStdErr(testName, problemsContent)
    tcLog.testFailed(testName, overview, "")
  }

  private fun getProblemsContent(problems: Set<CompatibilityProblem>): String =
      problems.groupBy({ it.shortDescription }, { it.fullDescription }).entries
          .joinToString(separator = "\n") { (short, fulls) ->
            "#$short\n" + fulls.joinToString(separator = "\n") { "    $it" }
          }

  private fun getMissingDependenciesProblemsContent(verificationResult: VerificationResult.MissingDependencies): String {
    val problems = verificationResult.problems
    val missingDependencies = verificationResult.directMissingDependencies

    val notFoundClassesProblems = problems.filterIsInstance<ClassNotFoundProblem>()
    return if (missingDependencies.isNotEmpty() && notFoundClassesProblems.size > 20) {
      getTooManyUnknownClassesProblems(missingDependencies, notFoundClassesProblems, problems)
    } else {
      getProblemsContent(problems)
    }
  }

  private fun getTooManyUnknownClassesProblems(missingDependencies: List<MissingDependency>,
                                               notFoundClassesProblems: List<ClassNotFoundProblem>,
                                               problems: Set<CompatibilityProblem>): String {
    val otherProblems: String = getProblemsContent(problems.filterNot { it in notFoundClassesProblems }.sortedBy { it.javaClass.name }.toSet())
    return "There are too much missing classes (${notFoundClassesProblems.size});\n" +
        "it's probably because of missing plugins (${missingDependencies.map { it.dependency }.joinToString()});\n" +
        "some not-found classes: [${notFoundClassesProblems.take(20).map { it.unresolved }.joinToString()}...];\n" +
        "\nrelevant problems: $otherProblems"
  }

  private fun StringBuilder.appendMissingDependencies(missingDeps: List<MissingDependency>) {
    missingDeps.forEach {
      append("Missing ${it.dependency}: ${it.missingReason}").append('\n')
    }
  }

  /**
   * For each [IDE version] [IdeVersion] returns the last versions
   * of the plugin available in the [repository] and compatible with
   * this IDE version.
   */
  private fun requestLastVersionsOfCheckedPlugins(ideVersions: List<IdeVersion>): Map<IdeVersion, List<PluginInfo>> =
      ideVersions.associate {
        try {
          val lastCompatibleUpdates = repository.getLastCompatiblePlugins(it)
          it to lastCompatibleUpdates.sortedByDescending { (it as UpdateInfo).updateId }.distinctBy { it.pluginId }
        } catch (e: Exception) {
          LOG.info("Unable to determine the last compatible updates of IDE $it")
          it to emptyList<UpdateInfo>()
        }
      }

  /**
   * Generates a TC test name in which the verification report will be printed.
   *
   * The test name is the [version] [PluginInfo.version] of the plugin
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
  private fun getPluginVersionAsTestName(pluginInfo: PluginInfo,
                                         ideVersion: IdeVersion,
                                         ideLastPluginVersions: Map<IdeVersion, List<PluginInfo>>) = with(pluginInfo) {
    val lastVersions = ideLastPluginVersions.getOrDefault(ideVersion, emptyList())
    if (this in lastVersions) {
      "($version - newest)"
    } else {
      "($version)"
    }
  }

  private fun groupByProblemType(results: List<VerificationResult>) {
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
            //todo: here and in other `testSuiteStarted` places
            //it would be better to wrap the string with (parantheses)
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

  private fun printMissingDependenciesAsTests(results: List<VerificationResult>) {
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