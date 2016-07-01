package com.jetbrains.pluginverifier.utils.teamcity

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.BrokenPluginProblem
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.results.ResultsElement
import com.jetbrains.pluginverifier.utils.MessageUtils
import com.jetbrains.pluginverifier.utils.ProblemUtils
import com.jetbrains.pluginverifier.utils.StringUtil
import org.apache.commons.cli.CommandLine
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy

fun main(args: Array<String>) {
  val log = TeamCityLog(PrintStream(FileOutputStream(File("for_tests/report.tc"))))
  val vPrinter = TeamCityVPrinter(log, TeamCityVPrinter.GroupBy.NOT_GROUPED)
  val problems: ResultsElement = ProblemUtils.loadProblems(File("for_tests/build-report.xml"))
  val ress = arrayListOf<VResult>()
  problems.asMap().forEach {
    val multimap = HashMultimap.create<Problem, ProblemLocation>()
    it.value.forEach {
      multimap.put(it, ProblemLocation.fromClass("some_class"))
    }
    ress.add(VResult.Problems(PluginDescriptor.ByUpdateInfo(it.key), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion(problems.ide)), "overview", multimap))
  }
  vPrinter.printResults(VResults(ress))
}

/**
 * @author Sergey Patrikeev
 */
class TeamCityVPrinter(val tcLog: TeamCityLog, val groupBy: GroupBy) : VPrinter {

  private val REPOSITORY_PLUGIN_ID_BASE = "https://plugins.jetbrains.com/plugin/index?xmlId="

  override fun printResults(results: VResults) {
    when (groupBy) {
      TeamCityVPrinter.GroupBy.NOT_GROUPED -> notGrouped(results)
      TeamCityVPrinter.GroupBy.BY_PROBLEM_TYPE -> groupByProblemType(results)
      TeamCityVPrinter.GroupBy.BY_PLUGIN -> groupByPlugin(results)
    }
  }

  private fun notGrouped(results: VResults) {
    //problem1 (in a:1.0, a:1.2, b:1.0)
    //problem2 (in a:1.0, c:1.3)

    val affected = ArrayListMultimap.create<Problem, PluginDescriptor>()

    results.results
        .forEach { res ->
          if (res is VResult.Problems) {
            res.problems.keySet().forEach {
              affected.put(it, res.pluginDescriptor)
            }
          }
        }

    affected.asMap().forEach {
      val plugins = it.value.sortedWith(compareBy<PluginDescriptor>({ it.pluginId }).thenBy { it.version })

      tcLog.buildProblem(MessageUtils.cutCommonPackages(it.key.description) + " (in " + plugins.joinToString { it.pluginId + ":" + it.version } + ")")
    }

  }


  private fun groupByPlugin(results: VResults) {
    //pluginOne
    //....(1.0)
    //........#invoking unknown method
    //............at someClass
    //........#accessing to unknown class
    //............at another class
    //....(1.2)
    //........#invoking unknown method
    //............at someClass
    //pluginTwo
    //...and so on...

    //request the last versions of the checked plugins. it is used to print "newest" suffix in the check-page.
    val lastUpdates = results.results.map { it.ideDescriptor.ideVersion }.distinct().associate {
      it to RepositoryManager.getInstance().getLastCompatibleUpdates(it).sortedByDescending { it.updateId }.distinctBy { Triple(it.pluginId, it.pluginName, it.version) }
    }

    results.results.groupBy { it.pluginDescriptor.pluginId }.forEach { pidToResults ->
      val pluginId = pidToResults.key
      val pluginLink = REPOSITORY_PLUGIN_ID_BASE + pluginId

      //<plugin_id>
      tcLog.testSuiteStarted(pluginId).use {

        pidToResults.value.groupBy { it.pluginDescriptor.version }.forEach { versionToResults ->

          //multiple plugins might have the same pluginIds and the same versions
          versionToResults.value.forEach { result ->

            val testName = genTestName(result.pluginDescriptor, result.ideDescriptor.ideVersion, lastUpdates)

            val problems: Multimap<Problem, ProblemLocation> = when (result) {
              is VResult.Nice -> ImmutableMultimap.of()
              is VResult.Problems -> result.problems
              is VResult.BadPlugin -> ImmutableMultimap.of(BrokenPluginProblem(result.overview), ProblemLocation.fromPlugin(pluginId))
            }

            tcLog.testStarted(testName).use {
              if (problems.isEmpty) {
                //nice
              } else {
                val sb = StringBuilder()
                for (entry in problems.asMap()) {
                  sb.append("#").append(entry.key.description).append("\n")
                  for (location in entry.value) {
                    sb.append("    at ").append(location).append("\n")
                  }
                }

                tcLog.testStdErr(testName, sb.toString())
                tcLog.testFailed(testName, "Plugin URL: $pluginLink\n" + "$pluginId:${result.pluginDescriptor.version} has ${problems.keySet().size} ${StringUtil.pluralize("problem", problems.keySet().size)}", "")
              }
            }

          }
        }

      }

    }
  }

  private fun genTestName(pluginDescriptor: PluginDescriptor, ideVersion: IdeVersion, lastUpdates: Map<IdeVersion, List<UpdateInfo>>): String {
    val relevant = lastUpdates[ideVersion] ?: return "(${pluginDescriptor.version})"
    val simple = "(${pluginDescriptor.version})"
    val newest = "(${pluginDescriptor.version} - newest)"
    return when (pluginDescriptor) {
      is PluginDescriptor.ByBuildId -> if (relevant.find { pluginDescriptor.buildId == it.updateId } != null) newest else simple
      is PluginDescriptor.ByUpdateInfo -> if (relevant.find { pluginDescriptor.updateInfo.updateId == it.updateId } != null) newest else simple
      else -> if (relevant.find { pluginDescriptor.pluginId == it.pluginId && pluginDescriptor.version == it.version } != null) newest else simple
    }
  }


  private fun groupByProblemType(results: VResults) {
    //accessing to unknown class SomeClass
    //....(pluginOne:1.2.0)
    //....(pluginTwo:2.0.0)
    //invoking unknown method method
    //....(pluginThree:1.0.0)

    val affected: Multimap<Problem, PluginDescriptor> = HashMultimap.create()
    results.results.forEach { result ->
      when (result) {
        is VResult.Nice -> {
        }
        is VResult.Problems -> result.problems.keySet().forEach { affected.put(it, result.pluginDescriptor) }
        is VResult.BadPlugin -> {
          affected.put(BrokenPluginProblem(result.overview), result.pluginDescriptor)
        }
      }
    }

    affected.keySet().groupBy { it.javaClass }.forEach { typeToProblems ->
      val prefix = convertNameToPrefix(typeToProblems.key)
      tcLog.testSuiteStarted("($prefix)").use {
        typeToProblems.value.forEach { problem ->
          affected.get(problem).forEach { plugin ->
            tcLog.testSuiteStarted(problem.description).use {
              val testName = "(${plugin.pluginId}:${plugin.version})"
              tcLog.testStarted(testName).use {
                val pluginUrl = REPOSITORY_PLUGIN_ID_BASE + plugin.pluginId
                tcLog.testFailed(testName, "Plugin URL: $pluginUrl\nPlugin: ${plugin.pluginId}:${plugin.version}", problem.description)
              }
            }
          }
        }
      }
    }
  }


  /**
   * Converts string like "com.some.package.name.MyClassNameProblem" to "my class name"
   */
  fun convertNameToPrefix(clazz: Class<Problem>): String {
    val name = clazz.name.substringAfterLast(".")
    var words = name.split("(?=[A-Z])".toRegex())
    if (words.size == 0) {
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

      fun parse(commandLine: CommandLine): GroupBy {
        val groupValue = commandLine.getOptionValue("g") ?: return NOT_GROUPED
        return values().find { it.arg == groupValue } ?: NOT_GROUPED
      }
    }
  }

}