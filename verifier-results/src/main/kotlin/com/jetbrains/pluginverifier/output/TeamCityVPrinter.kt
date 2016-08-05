package com.jetbrains.pluginverifier.output

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiCompareResult
import com.jetbrains.pluginverifier.configurations.MissingCompatibleUpdate
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.RepositoryManager
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy


/**
 * @author Sergey Patrikeev
 */
class TeamCityVPrinter(val tcLog: TeamCityLog, val groupBy: GroupBy) : VPrinter {

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

      tcLog.buildProblem(it.key.getDescription() + " (in " + plugins.joinToString { it.pluginId + ":" + it.version } + ")")
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
                    "#${it.key.getDescription()}\n" +
                        it.value.joinToString(separator = "\n", prefix = "    #")
                  })
                  tcLog.testFailed(testName, "Plugin URL: $pluginLink\n" + "$pluginId:${result.pluginDescriptor.version} has ${result.problems.keySet().size} ${"problem".pluralize(result.problems.keySet().size)}", "")

                }
                is VResult.BadPlugin -> {
                  tcLog.testStdErr(testName, "Plugin is invalid: ${result.reason}")
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
      is PluginDescriptor.ByUpdateInfo -> if (relevant.find { pluginDescriptor.updateInfo.updateId == it.updateId } != null) newest else simple
      else -> if (relevant.find { pluginDescriptor.pluginId == it.pluginId && pluginDescriptor.version == it.version } != null) newest else simple
    }
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
    val problemToUpdates: Multimap<Problem, UpdateInfo> = Multimaps.invertFrom(compareResult.newProblems, ArrayListMultimap.create())

    compareResult.newProblems.values().distinct().groupBy { it.javaClass }.forEach { typeToProblems ->
      val prefix = convertNameToPrefix(typeToProblems.key)
      tcLog.testSuiteStarted("($prefix)").use {
        typeToProblems.value.forEach { problem ->
          tcLog.testSuiteStarted(problem.getDescription()).use {
            problemToUpdates.get(problem).forEach { plugin ->
              val testName = "(${plugin.pluginId}:${plugin.version})"
              tcLog.testStarted(testName).use {
                val pluginUrl = REPOSITORY_PLUGIN_ID_BASE + plugin.pluginId
                tcLog.testFailed(testName, "Plugin URL: $pluginUrl\nPlugin: ${plugin.pluginId}:${plugin.version}", problem.getDescription())
              }
            }
          }
        }
      }
    }

    //print missing dependencies
    tcLog.testSuiteStarted("missing plugin dependencies").use {
      compareResult.newMissingProblems.asMap().entries.forEach { missingToProblems ->
        val type = if (missingToProblems.key.pluginId.startsWith("com.intellij.modules")) "module" else "plugin"
        val testName = "(missing $type ${missingToProblems.key})"
        tcLog.testStarted(testName).use {
          tcLog.testFailed(testName, "$type ${missingToProblems.key} is not found in ${compareResult.currentVersion} " +
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
            tcLog.testSuiteStarted(problem.getDescription()).use {
              val testName = "(${plugin.pluginId}:${plugin.version})"
              tcLog.testStarted(testName).use {
                val pluginUrl = REPOSITORY_PLUGIN_ID_BASE + plugin.pluginId
                tcLog.testFailed(testName, "Plugin URL: $pluginUrl\nPlugin: ${plugin.pluginId}:${plugin.version}", problem.getDescription())
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
      fun parse(groupValue: String?): GroupBy {
        groupValue ?: return NOT_GROUPED
        return values().find { it.arg == groupValue } ?: NOT_GROUPED
      }
    }
  }

}