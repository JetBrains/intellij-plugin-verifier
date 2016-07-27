package com.jetbrains.pluginverifier.output

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.configurations.CheckIdeCompareResult
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.persistence.multimapFromMap
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem
import com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.MessageUtils
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy


/**
 * @author Sergey Patrikeev
 */
class TeamCityVPrinter(val tcLog: TeamCityLog, val groupBy: GroupBy) : VPrinter {

  private val REPOSITORY_PLUGIN_ID_BASE = "https://plugins.jetbrains.com/plugin/index?xmlId="

  fun printNoCompatibleUpdatesProblems(problems: List<NoCompatibleUpdatesProblem>) {
    when (groupBy) {
      TeamCityVPrinter.GroupBy.NOT_GROUPED -> {
        problems.forEach { tcLog.buildProblem(it.description) }
      }
      TeamCityVPrinter.GroupBy.BY_PLUGIN -> {
        problems.forEach { problem ->
          tcLog.testSuiteStarted(problem.plugin).use {
            val testName = "(no compatible update)"
            tcLog.testStarted(testName).use {
              tcLog.testStdErr(testName, "#${problem.description}\n")
              tcLog.testFailed(testName, "Plugin URL: ${REPOSITORY_PLUGIN_ID_BASE + problem.plugin}\n", "")
            }
          }
        }
      }
      TeamCityVPrinter.GroupBy.BY_PROBLEM_TYPE -> {
        tcLog.testSuiteStarted("(no compatible update)").use {
          problems.forEach { problem ->
            tcLog.testSuiteStarted(problem.plugin).use {
              val testName = problem.plugin
              tcLog.testStarted(testName).use {
                tcLog.testFailed(testName, "Plugin URL: ${REPOSITORY_PLUGIN_ID_BASE + problem.plugin}\n", problem.description)
              }
            }
          }
        }
      }
    }

  }


  override fun printResults(results: VResults) {
    when (groupBy) {
      GroupBy.NOT_GROUPED -> notGrouped(results)
      GroupBy.BY_PROBLEM_TYPE -> groupByProblemType(results)
      GroupBy.BY_PLUGIN -> groupByPlugin(results)
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

            tcLog.testStarted(testName).use {
              when (result) {
                is VResult.Nice -> {/*test is passed.*/
                }
                is VResult.Problems -> {

                  tcLog.testStdErr(testName, result.problems.asMap().entries.joinToString(separator = "\n") {
                    "#${it.key.description}\n" +
                        it.value.joinToString(separator = "\n", prefix = "    #")
                  })
                  tcLog.testFailed(testName, "Plugin URL: $pluginLink\n" + "$pluginId:${result.pluginDescriptor.version} has ${result.problems.keySet().size} ${"problem".pluralize(result.problems.keySet().size)}", "")

                }
                is VResult.BadPlugin -> {
                  tcLog.testStdErr(testName, "Plugin is invalid: ${result.overview}")
                }
                is VResult.NotFound -> {
                  /*Suppose it is ok*/
                }

              }
            }

          }
        }

      }

    }
  }

  private fun String.pluralize(times: Int): String {
    if (times < 0) throw IllegalArgumentException("Negative value")
    if (times == 0) return ""
    if (times == 1) return this else return this + "s"
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

  fun printIdeCompareResult(compareResult: CheckIdeCompareResult) {
    //since IU-162.100
    //....accessing to unknown class
    //........firstClass
    //.............pluginOne:1.0
    //.............pluginOne:1.2
    //.............pluginTwo:2.1
    //........secondClass
    //.............pluginOne:1.0
    //....invoking unknown method
    //........myUnknownMethod
    //missing plugin dependencies
    //....(missing module oneModule)
    //.........oneModule is required for plugin#1, plugin#2
    //....(missing plugin onePlugin)
    //.........onePlugin is required for plugin#1
    //...and so on....
    val problemToUpdates: Multimap<Problem, UpdateInfo> = Multimaps.invertFrom(compareResult.pluginProblems, ArrayListMultimap.create())
    val ideToProblems: Multimap<IdeVersion, Problem> = compareResult.firstOccurrences.entries.groupBy({ it.value }, { it.key }).multimapFromMap()

    ideToProblems.keySet().sorted().forEach { ide ->
      tcLog.testSuiteStarted("(since $ide)").use {
        ideToProblems[ide].filterNot { it is MissingDependencyProblem }.groupBy { it.javaClass }.forEach { typeToProblems ->
          val prefix = convertNameToPrefix(typeToProblems.key)
          tcLog.testSuiteStarted("($prefix)").use {
            typeToProblems.value.forEach { problem ->
              tcLog.testSuiteStarted(problem.description).use {
                problemToUpdates.get(problem).forEach { plugin ->
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
    }

    //print missing dependencies
    tcLog.testSuiteStarted("missing plugin dependencies").use {
      problemToUpdates
          .keySet()
          .filter { it is MissingDependencyProblem }
          .map { it as MissingDependencyProblem }
          .groupBy { it.missingId }
          .forEach { missingToProblems ->
            val type = if (missingToProblems.key.startsWith("com.intellij.modules")) "module" else "plugin"
            val testName = "(missing $type ${missingToProblems.key})"
            tcLog.testStarted(testName).use {
              tcLog.testFailed(testName, "$type ${missingToProblems.key} is not found in ${compareResult.checkIdeVersion} " +
                  "but it is required for the following plugins: [${missingToProblems.value.map { it.plugin }.joinToString()}]", "")
            }
          }
    }


    //TODO: maybe don't count missing dependencies here?
    val newProblemsCnt = ideToProblems.get(compareResult.checkIdeVersion).size
    val totalProblemsCnt = problemToUpdates.keySet().size
    val allCheckedIdes: List<IdeVersion> = ideToProblems.keySet().sorted()
    val text = "Done, %d new %s in %s; %d problems between %s and %s".format(newProblemsCnt, "problem".pluralize(newProblemsCnt), compareResult.checkIdeVersion.asString(), totalProblemsCnt, allCheckedIdes.first(), allCheckedIdes.last())
    if (newProblemsCnt > 0) {
      tcLog.buildStatusFailure(text)
    } else {
      tcLog.buildStatusSuccess(text)
    }
  }

  private fun groupByProblemType(results: VResults) {
    //accessing to unknown class SomeClass
    //....(pluginOne:1.2.0)
    //....(pluginTwo:2.0.0)
    //invoking unknown method method
    //....(pluginThree:1.0.0)
    class BrokenPluginProblem(private val description: String) : Problem() {
      override fun getDescription(): String = description
    }

    val affected: Multimap<Problem, PluginDescriptor> = HashMultimap.create()
    results.results.forEach { result ->
      when (result) {
        is VResult.Nice -> {
        }
        is VResult.Problems -> result.problems.keySet().forEach { affected.put(it, result.pluginDescriptor) }
        is VResult.BadPlugin -> {
          affected.put(BrokenPluginProblem(result.overview), result.pluginDescriptor)
        }
        is VResult.NotFound -> {

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

      @JvmStatic
      fun parse(opts: CmdOpts): GroupBy {
        val groupValue = opts.group ?: return NOT_GROUPED
        return values().find { it.arg == groupValue } ?: NOT_GROUPED
      }
    }
  }

}