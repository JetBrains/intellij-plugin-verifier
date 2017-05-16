package com.jetbrains.pluginverifier.output

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiCompareResult
import com.jetbrains.pluginverifier.configurations.MissingCompatibleUpdate
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * @author Sergey Patrikeev
 */
class TeamCityVPrinter(private val tcLog: TeamCityLog,
                       private val groupBy: GroupBy,
                       private val repository: PluginRepository = RepositoryManager) : Printer {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(TeamCityVPrinter::class.java)
  }

  private val REPOSITORY_PLUGIN_ID_BASE = "https://plugins.jetbrains.com/plugin/index?xmlId="

  fun printNoCompatibleUpdatesProblems(problems: List<MissingCompatibleUpdate>) {
    when (groupBy) {
      TeamCityVPrinter.GroupBy.NOT_GROUPED -> {
        problems.forEach { tcLog.buildProblem(it.toString()) }
      }
      TeamCityVPrinter.GroupBy.BY_PLUGIN -> {
        problems.forEach { problem ->
          tcLog.testSuiteStarted(problem.pluginId).use {
            val testName = "(no compatible update)"
            tcLog.testStarted(testName).use {
              tcLog.testStdErr(testName, "#$problem\n")
              tcLog.testFailed(testName, "Plugin URL: ${REPOSITORY_PLUGIN_ID_BASE + problem.pluginId}\n", "")
            }
          }
        }
      }
      TeamCityVPrinter.GroupBy.BY_PROBLEM_TYPE -> {
        tcLog.testSuiteStarted("(no compatible update)").use {
          problems.forEach { problem ->
            tcLog.testSuiteStarted(problem.pluginId).use {
              val testName = problem.pluginId
              tcLog.testStarted(testName).use {
                tcLog.testFailed(testName, "Plugin URL: ${REPOSITORY_PLUGIN_ID_BASE + problem.pluginId}\n", problem.toString())
              }
            }
          }
        }
      }
    }

  }


  override fun printResults(results: List<Result>, options: PrinterOptions) {
    when (groupBy) {
      GroupBy.NOT_GROUPED -> notGrouped(results)
      GroupBy.BY_PROBLEM_TYPE -> groupByProblemType(results)
      GroupBy.BY_PLUGIN -> groupByPlugin(results, options)
    }
  }

  private fun notGrouped(results: List<Result>) {
    //problem1 (in a:1.0, a:1.2, b:1.0)
    //problem2 (in a:1.0, c:1.3)
    //missing dependencies: missing#1 (required for plugin1, plugin2, plugin3)
    //missing dependencies: missing#2 (required for plugin2, plugin4)

    printProblemAndAffectedPluginsAsBuildProblem(results)
    printMissingDependenciesAndRequiredPluginsAsBuildProblem(results)
  }

  private fun printProblemAndAffectedPluginsAsBuildProblem(results: List<Result>) {
    val shortDescriptionToResults: Multimap<String, Result> = HashMultimap.create()
    results.forEach { result ->
      getProblemsOfVerdict(result.verdict).forEach {
        shortDescriptionToResults.put(it.getShortDescription(), result)
      }
    }
    shortDescriptionToResults.asMap().forEach { description, results ->
      val allPluginsWithThisProblem = results.map { it.plugin }
      tcLog.buildProblem(description + " (in ${allPluginsWithThisProblem.joinToString()})")
    }
  }

  private fun getProblemsOfVerdict(verdict: Verdict): Collection<Problem> = when (verdict) {
    is Verdict.Problems -> verdict.problems
    is Verdict.MissingDependencies -> verdict.problems
    is Verdict.OK -> emptyList()
    is Verdict.Warnings -> emptyList()
    is Verdict.Bad -> emptyList()
    is Verdict.NotFound -> emptyList()
  }

  private fun printMissingDependenciesAndRequiredPluginsAsBuildProblem(results: List<Result>) {
    val missingToRequired = collectMissingDependenciesForRequiringPlugins(results)
    missingToRequired.asMap().entries.forEach {
      tcLog.buildProblem("Missing dependency ${it.key} (required for ${it.value.joinToString()})")
    }
  }

  private fun collectMissingDependenciesForRequiringPlugins(results: List<Result>): Multimap<MissingDependency, PluginInfo> {
    val missingToRequiring = HashMultimap.create<MissingDependency, PluginInfo>()
    results.filter { it.verdict is Verdict.MissingDependencies }.forEach {
      (it.verdict as Verdict.MissingDependencies).missingDependencies.forEach { missingDependency ->
        missingToRequiring.put(missingDependency, it.plugin)
      }
    }
    return missingToRequiring
  }


  private fun groupByPlugin(results: List<Result>, options: PrinterOptions) {
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
    val lastUpdates: Map<IdeVersion, List<UpdateInfo>> = requestLastVersionsOfCheckedPlugins(results)
    results.groupBy { it.plugin.pluginId }.forEach { (pluginId, pluginResults) ->
      printResultsForSpecificPluginId(options, pluginId, pluginResults, lastUpdates)
    }
  }

  private fun printResultsForSpecificPluginId(options: PrinterOptions,
                                              pluginId: String,
                                              pluginResults: List<Result>,
                                              lastUpdates: Map<IdeVersion, List<UpdateInfo>>) {
    tcLog.testSuiteStarted(pluginId).use {
      pluginResults.groupBy { it.plugin.version }.forEach { versionToVerdicts ->
        versionToVerdicts.value.forEach { (plugin, ideVersion, verdict) ->
          val testName = genTestName(plugin, ideVersion, lastUpdates)
          printResultOfSpecificVersion(plugin, verdict, testName, options)
        }
      }
    }
  }

  private fun printResultOfSpecificVersion(plugin: PluginInfo,
                                           verdict: Verdict,
                                           testName: String,
                                           options: PrinterOptions) {
    tcLog.testStarted(testName).use {
      when (verdict) {
        is Verdict.Problems -> printProblems(plugin, testName, verdict.problems)
        is Verdict.MissingDependencies -> printMissingDependencies(plugin, verdict, testName, options)
        is Verdict.Bad -> printBadPluginResult(verdict, testName)
      }
    }
  }

  private fun printMissingDependencies(plugin: PluginInfo,
                                       verdict: Verdict.MissingDependencies,
                                       testName: String,
                                       options: PrinterOptions) {
    val problems = verdict.problems
    val pluginLink = getPluginLink(plugin)
    val overview = "Plugin URL: $pluginLink" + "\n" +
        "$plugin has ${problems.size} ${"problem".pluralize(problems.size)}" + "\n" +
        getMissingDependenciesOverview(options, verdict) + "\n"
    val problemsContent = getMissingDependenciesProblemsContent(verdict)
    tcLog.testStdErr(testName, overview)
    tcLog.testFailed(testName, problemsContent, "")
  }

  private fun printBadPluginResult(verdict: Verdict.Bad, versionTestName: String) {
    val message = "Plugin is invalid: ${verdict.reason}"
    tcLog.testStdErr(versionTestName, message)
    tcLog.testFailed(versionTestName, message, "")
  }

  private fun printProblems(plugin: PluginInfo,
                            testName: String,
                            problems: Set<Problem>) {
    val pluginLink = getPluginLink(plugin)
    val overview = "Plugin URL: $pluginLink\n$plugin has ${problems.size} ${"problem".pluralize(problems.size)}\n"
    val problemsContent = getProblemsContent(problems)
    tcLog.testStdErr(testName, problemsContent)
    tcLog.testFailed(testName, overview, "")
  }

  private fun getProblemsContent(problems: Set<Problem>): String = problems.joinToString(separator = "\n") {
    "#${it.getShortDescription()}\n    ${it.getFullDescription()}"
  }

  private fun getMissingDependenciesProblemsContent(verdict: Verdict.MissingDependencies): String {
    val problems = verdict.problems
    val missingDependencies = verdict.missingDependencies

    val notFoundClassesProblems = problems.filterIsInstance<ClassNotFoundProblem>()
    if (missingDependencies.isNotEmpty() && notFoundClassesProblems.size > 20) {
      return getTooMuchUnknownClassesWarning(missingDependencies, notFoundClassesProblems, problems)
    } else {
      return getProblemsContent(problems)
    }
  }

  private fun getTooMuchUnknownClassesWarning(missingDependencies: List<MissingDependency>,
                                              notFoundClassesProblems: List<ClassNotFoundProblem>,
                                              problems: Set<Problem>): String {
    val otherProblems: String = getProblemsContent(problems.filterNot { it in notFoundClassesProblems }.sortedBy { it.javaClass.name }.toSet())
    return "There are too much missing classes (${notFoundClassesProblems.size});\n" +
        "it's probably because of missing plugins (${missingDependencies.map { it.dependency }.joinToString()});\n" +
        "the following classes are not found: [${notFoundClassesProblems.map { it.unresolved }.joinToString()}];\n" +
        otherProblems
  }

  private fun getMissingDependenciesOverview(options: PrinterOptions, verdict: Verdict.MissingDependencies): String {
    val builder = StringBuilder()
    builder.append("Some problems might be caused by missing plugins:").append('\n')
    verdict.missingDependencies.filterNot { it.dependency.isOptional }.forEach { builder.append("    $it").append('\n') }
    verdict.missingDependencies.filter { it.dependency.isOptional && !options.ignoreMissingOptionalDependency(it.dependency) }.forEach {
      builder.append("Missing ${it.dependency}: ${it.missingReason}").append('\n')
    }
    return builder.toString()
  }

  private fun requestLastVersionsOfCheckedPlugins(results: List<Result>): Map<IdeVersion, List<UpdateInfo>> =
      results
          .map { it.ideVersion }
          .distinct()
          .associate { ideVersion ->
            try {
              val lastCompatibleUpdates = repository.getLastCompatibleUpdates(ideVersion)
              ideVersion to lastCompatibleUpdates.sortedByDescending { it.updateId }.distinctBy { it.pluginId }
            } catch(e: Exception) {
              LOG.info("Unable to determine the last compatible updates of IDE $ideVersion")
              ideVersion to emptyList<UpdateInfo>()
            }
          }

  private fun genTestName(pluginInfo: PluginInfo,
                          ideVersion: IdeVersion,
                          lastUpdates: Map<IdeVersion, List<UpdateInfo>>): String {
    val onlyVersion = "(${pluginInfo.version})"
    val relevant = lastUpdates[ideVersion] ?: return onlyVersion
    val newest = "(${pluginInfo.version} - newest)"
    if (pluginInfo.updateInfo != null) {
      return if (relevant.any { pluginInfo.updateInfo.updateId == it.updateId }) newest else onlyVersion
    }
    return if (relevant.find { pluginInfo.pluginId == it.pluginId && pluginInfo.version == it.version } != null) newest else onlyVersion
  }

  fun printIdeCompareResult(compareResult: CheckTrunkApiCompareResult) {
    //accessing to unknown class
    //....firstClass
    //.........pluginOne:1.0
    //.........pluginOne:1.2
    //.........pluginTwo:2.1
    //....secondClass
    //.........pluginOne:1.0
    //invoking unknown method
    //....myUnknownMethod
    //.........pluginThree:1.3
    //missing plugin dependencies
    //....(missing module oneModule)
    //.........oneModule is required for plugin#1, plugin#2
    //....(missing plugin onePlugin)
    //.........onePlugin is required for plugin#1
    //...and so on....
    val problemToUpdates: Multimap<Problem, UpdateInfo> = Multimaps.invertFrom(compareResult.newProblems, HashMultimap.create())

    compareResult.newProblems.values().distinct().groupBy { it.javaClass }.forEach { typeToProblems ->
      val prefix = convertProblemClassNameToSentence(typeToProblems.key)
      tcLog.testSuiteStarted("($prefix)").use {
        typeToProblems.value.forEach { problem ->
          tcLog.testSuiteStarted(problem.getShortDescription()).use {
            problemToUpdates.get(problem).forEach { plugin ->
              val testName = "($plugin)"
              tcLog.testStarted(testName).use {
                val pluginUrl = getPluginLink(plugin)
                tcLog.testFailed(testName, "Plugin URL: $pluginUrl\nPlugin: ${plugin.pluginId}:${plugin.version}", problem.getFullDescription())
              }
            }
          }
        }
      }
    }

    //print missing dependencies
    tcLog.testSuiteStarted("missing plugin dependencies").use {
      compareResult.newMissingProblems.asMap().entries.forEach { missingToProblems ->
        val missingDependency = missingToProblems.key
        val testName = "(missing $missingDependency)"
        tcLog.testStarted(testName).use {
          tcLog.testFailed(testName, "$missingDependency is not found in ${compareResult.currentVersion} " +
              "but it is required for the following plugins: [${missingToProblems.value.map { it.pluginId }.joinToString()}]", "")
        }
      }
    }


    val newProblemsCnt = problemToUpdates.keySet().size + compareResult.newMissingProblems.keySet().size
    val text = "Done, %d new %s in %s compared to %s".format(newProblemsCnt, "problem".pluralize(newProblemsCnt), compareResult.currentVersion, compareResult.majorVersion)
    if (newProblemsCnt > 0) {
      tcLog.buildStatusFailure(text)
    } else {
      tcLog.buildStatusSuccess(text)
    }
  }

  private fun groupByProblemType(results: List<Result>) {
    //accessing to unknown class SomeClass
    //....(pluginOne:1.2.0)
    //....(pluginTwo:2.0.0)
    //invoking unknown method method
    //....(pluginThree:1.0.0)
    //missing dependencies
    //....(missing#1)
    //.........Required for plugin1, plugin2, plugin3
    val problemToDetectingResult: Multimap<Problem, Result> = HashMultimap.create()
    results.forEach { result ->
      getProblemsOfVerdict(result.verdict).forEach {
        problemToDetectingResult.put(it, result)
      }
    }

    problemToDetectingResult.keySet().groupBy { it.javaClass }.forEach { typeToProblems ->
      val prefix = convertProblemClassNameToSentence(typeToProblems.key)
      tcLog.testSuiteStarted("($prefix)").use {
        typeToProblems.value.forEach { problem ->
          problemToDetectingResult.get(problem).forEach { (plugin) ->
            tcLog.testSuiteStarted(problem.getShortDescription()).use {
              val testName = "($plugin)"
              tcLog.testStarted(testName).use {
                val pluginUrl = getPluginLink(plugin)
                tcLog.testFailed(testName, "Plugin URL: $pluginUrl\nPlugin: $plugin", problem.getFullDescription())
              }
            }
          }
        }
      }
    }

    printMissingDependenciesAsTests(results)
  }

  private fun getPluginLink(pluginInfo: PluginInfo): String = REPOSITORY_PLUGIN_ID_BASE + pluginInfo.pluginId

  private fun getPluginLink(updateInfo: UpdateInfo): String = REPOSITORY_PLUGIN_ID_BASE + updateInfo.pluginId

  private fun printMissingDependenciesAsTests(results: List<Result>) {
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


  /**
   * Converts string like "com.some.package.name.MyClassNameProblem" to "my class name"
   */
  private fun convertProblemClassNameToSentence(clazz: Class<Problem>): String {
    val name = clazz.name.substringAfterLast(".")
    var words = name.split("(?=[A-Z])".toRegex())
    if (words.isEmpty()) {
      return name.toLowerCase()
    }
    if (words.last() == "Problem") {
      words = words.dropLast(1)
    }
    return words.map { it.toLowerCase() }.joinToString(" ")
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