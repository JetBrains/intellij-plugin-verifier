package com.jetbrains.pluginverifier.utils.teamcity

import com.google.common.base.Joiner
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.jetbrains.pluginverifier.commands.CheckIdeCommand
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.results.ProblemSet
import com.jetbrains.pluginverifier.utils.FailUtil
import com.jetbrains.pluginverifier.utils.MessageUtils
import com.jetbrains.pluginverifier.utils.ProblemUtils
import com.jetbrains.pluginverifier.utils.StringUtil
import com.jetbrains.pluginverifier.utils.StringUtil.pluralize
import org.apache.commons.cli.CommandLine
import java.util.*
import java.util.function.Predicate

/**
 * @author Sergey Evdokimov
 */
object TeamCityUtil {

  private val REPOSITORY_PLUGIN_ID_BASE = "https://plugins.jetbrains.com/plugin/index?xmlId="

  private fun notGrouped(log: TeamCityLog, problems: Multimap<Problem, UpdateInfo>) {
    val sortedProblems = ProblemUtils.sortProblems(problems.keySet())

    for (problem in sortedProblems) {
      val updates = ProblemUtils.sortUpdatesWithDescendingVersionsOrder(problems.get(problem))

      log.buildProblem(MessageUtils.cutCommonPackages(problem.description) + " (in " + Joiner.on(", ").join(updates) + ')')
    }
  }

  fun printReportWithLocations(log: TeamCityLog, results: Map<UpdateInfo, ProblemSet>) {
    if (log === TeamCityLog.NULL_LOG) return
    if (results.isEmpty()) return

    groupByPlugin(log, results)
  }

  fun printReport(log: TeamCityLog,
                  problems: Multimap<Problem, UpdateInfo>,
                  reportGrouping: ReportGrouping) {
    if (log === TeamCityLog.NULL_LOG) return
    if (problems.isEmpty) return

    val inverted = invertMultimap(problems)

    when (reportGrouping) {
      ReportGrouping.NONE -> notGrouped(log, problems)
      ReportGrouping.PLUGIN -> groupByPlugin(log, fillWithEmptyLocations(inverted.asMap()))
      ReportGrouping.PROBLEM_TYPE -> groupByType(log, inverted.asMap())
      ReportGrouping.PLUGIN_WITH_LOCATION -> groupByPlugin(log, fillWithEmptyLocations(inverted.asMap()))
    }
  }

  fun fillWithEmptyLocations(map: Map<UpdateInfo, Collection<Problem>>): Map<UpdateInfo, ProblemSet> {
    val result = HashMap<UpdateInfo, ProblemSet>()

    val emptySet = emptySet<ProblemLocation>()

    for (entry in map.entries) {
      val problemMap = HashMap<Problem, Set<ProblemLocation>>()

      for (problem in entry.value) {
        problemMap.put(problem, emptySet)
      }
      result.put(entry.key, ProblemSet(problemMap))

    }
    return result
  }

  private fun invertMultimap(problem2Updates: Multimap<Problem, UpdateInfo>): Multimap<UpdateInfo, Problem> {
    val result = ArrayListMultimap.create<UpdateInfo, Problem>()
    Multimaps.invertFrom(problem2Updates, result)
    return result
  }

  private fun getPluginUrl(updateInfo: UpdateInfo): String {
    return REPOSITORY_PLUGIN_ID_BASE + if (updateInfo.pluginId != null) updateInfo.pluginId else updateInfo.pluginName
  }

  fun groupByType(log: TeamCityLog, map: Map<UpdateInfo, Collection<Problem>>) {
    val problem2Updates = ProblemUtils.flipProblemsMap(map)

    val problemType2Problem = ArrayListMultimap.create<String, Problem>()
    for (problem in problem2Updates.keySet()) {
      problemType2Problem.put(problem.javaClass.canonicalName, problem)
    }

    for (problemType in problemType2Problem.keySet()) {
      val problems = ProblemUtils.sortProblems(problemType2Problem.get(problemType))

      if (problems.isEmpty()) continue

      val prefix = convertNameToPrefix(problemType)
      val problemTypeSuite = log.testSuiteStarted("($prefix)")

      for (problem in problems) {
        val description = StringUtil.trimStart(problem.description, prefix).trim { it <= ' ' }
        val updateInfos = problem2Updates.get(problem)

        val problemSuite = log.testSuiteStarted("[$description]")

        for (updateInfo in updateInfos) {
          val plugin = "(" + updateInfo.pluginId + "-" + updateInfo.version + ")"
          val test = log.testStarted(plugin)
          val pluginUrl = getPluginUrl(updateInfo)
          log.testFailed(plugin, "Plugin URL: $pluginUrl\nPluginId: $updateInfo", problem.description)
          test.close()
        }

        problemSuite.close()
      }

      problemTypeSuite.close()
    }

  }

  fun convertNameToPrefix(className: String): String {
    val name = className.substringAfterLast(".")
    var words = name.split("(?=[A-Z])".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    if (words.size == 0) {
      return name
    }
    if (words[words.size - 1] == "Problem") {
      words = Arrays.copyOf(words, words.size - 1)
    }
    return words.map { it.toLowerCase() }.joinToString(" ")
  }


  private fun fillIdToUpdates(map: Map<UpdateInfo, ProblemSet>): Multimap<String, UpdateInfo> {
    val idToUpdates = ArrayListMultimap.create<String, UpdateInfo>()
    for (updateInfo in map.keys) {
      val pluginId = if (updateInfo.pluginId != null) updateInfo.pluginId else "#" + updateInfo.updateId!!
      idToUpdates.put(pluginId, updateInfo)
    }
    return idToUpdates
  }

  fun groupByPlugin(log: TeamCityLog, map: Map<UpdateInfo, ProblemSet>) {

    val idToUpdates = fillIdToUpdates(map)

    for (pluginId in idToUpdates.keySet()) {
      val updateInfos = ProblemUtils.sortUpdatesWithDescendingVersionsOrder(idToUpdates.get(pluginId))

      val pluginLink = REPOSITORY_PLUGIN_ID_BASE + pluginId
      val pluginSuite = log.testSuiteStarted(pluginId)

      for (updateInfo in updateInfos) {

        val problemToLocations = map[updateInfo]!!.asMap()

        val problems = ProblemUtils.sortProblems(problemToLocations.keys)

        val version = if (updateInfo.version != null) updateInfo.version else "#" + updateInfo.updateId!!
        val testName = "(" + version + (if (updateInfo === updateInfos[0] && CheckIdeCommand.NO_COMPATIBLE_UPDATE_VERSION != version) " - newest" else "") + ")"

        if (problems.isEmpty()) {
          //plugin has no problems => test passed.
          val test = log.testStarted(testName)
          test.close()
        } else {

          val builder = StringBuilder()

          for (problem in problems) {
            builder.append("#").append(problem.description).append("\n")

            for (location in problemToLocations[problem]!!) {
              builder.append("      at ").append(location).append("\n")
            }
          }

          val test = log.testStarted(testName)
          log.testStdErr(testName, builder.toString())
          log.testFailed(testName, "Plugin URL: " + pluginLink + '\n' + updateInfo + " has " + problems.size + " " + pluralize("problem", problems.size), "")
          test.close()

        }
      }
      pluginSuite.close()
    }
  }

  /**
   * Prints Build Problems in the Overview page or as tests
   */
  fun printTeamCityProblems(log: TeamCityLog,
                            results: Map<UpdateInfo, ProblemSet>,
                            updateFilter: Predicate<UpdateInfo>,
                            reportGrouping: ReportGrouping) {
    if (log === TeamCityLog.NULL_LOG) return

    //list of problems without their exact problem location (only affected plugin)
    val problems = ArrayListMultimap.create<Problem, UpdateInfo>()

    //fill problems map
    for (entry in results.entries) {
      if (!updateFilter.test(entry.key)) {
        continue //this is excluded plugin
      }

      for (problem in entry.value.allProblems) {
        problems.put(problem, entry.key)
      }
    }

    if (reportGrouping == ReportGrouping.PLUGIN_WITH_LOCATION) {
      val keys = results.filterKeys { updateFilter.test(it) }
      printReportWithLocations(log, keys)
    } else {
      printReport(log, problems, reportGrouping)
    }
  }


  enum class ReportGrouping(private val myText: String) {
    PLUGIN("plugin"),
    PROBLEM_TYPE("type"),
    NONE(""),
    PLUGIN_WITH_LOCATION("plugin_with_location");


    companion object {

      fun parseGrouping(commandLine: CommandLine): ReportGrouping {
        val grouping = NONE
        val groupValue = commandLine.getOptionValue("g")
        if (groupValue != null) {
          for (report in values()) {
            if (report.myText == groupValue) {
              return report
            }
          }
          throw FailUtil.fail("Grouping argument should be one of 'plugin', 'type', 'plugin_with_location' or not being set at all.")
        }
        return grouping
      }
    }
  }
}
