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
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy


/**
 * @author Sergey Patrikeev
 */
class TeamCityVPrinter(val tcLog: TeamCityLog,
                       val groupBy: GroupBy,
                       val pluginRepository: PluginRepository = RepositoryManager) : VPrinter {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(TeamCityVPrinter::class.java)
  }

  private val REPOSITORY_PLUGIN_ID_BASE = "https://plugins.jetbrains.com/plugin/index?xmlId="

  private val INTELLIJ_MODULES_PREFIX = "com.intellij.modules"

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


  override fun printResults(results: VResults, options: VPrinterOptions) {
    when (groupBy) {
      GroupBy.NOT_GROUPED -> notGrouped(results)
      GroupBy.BY_PROBLEM_TYPE -> groupByProblemType(results)
      GroupBy.BY_PLUGIN -> groupByPlugin(results, options)
    }
  }

  private fun notGrouped(results: VResults) {
    //problem1 (in a:1.0, a:1.2, b:1.0)
    //problem2 (in a:1.0, c:1.3)
    //missing dependencies: missing#1 (required for plugin1, plugin2, plugin3)
    //missing dependencies: missing#2 (required for plugin2, plugin4)

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

    results.results.filterIsInstance<VResult.Problems>()
        .flatMap { descr -> descr.dependenciesGraph.getMissingNonOptionalDependencies().map { it.missing.id to descr.pluginDescriptor } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.isNotEmpty() }
        .apply {
          if (this.isNotEmpty()) {
            this.forEach {
              tcLog.buildProblem("Missing dependency ${it.key} (required for ${it.value.joinToString()})")
            }
          }
        }

  }


  private fun groupByPlugin(results: VResults, options: VPrinterOptions) {
    //pluginOne
    //....(1.0)
    //........#invoking unknown method
    //............at someClass
    //........#accessing to unknown class
    //............at another class
    //....(1.2)
    //........#invoking unknown method
    //............at someClass
    //........missing non-optional dependency dep#1
    //........missing non-optional dependency pluginOne:1.2 -> otherPlugin:3.3 -> dep#2 (it doesn't have a compatible build with IDE #IU-162.1121.10)
    //........missing optional dependency dep#3
    //pluginTwo
    //...and so on...

    val lastUpdates = requestLatestVersionsOfUpdatesForEachCheckedIde(results.results.map { it.ideDescriptor.ideVersion }.distinct())

    results.results.groupBy { it.pluginDescriptor.pluginId }.forEach { pidToResults ->
      val pluginId = pidToResults.key
      val pluginLink = REPOSITORY_PLUGIN_ID_BASE + pluginId

      //<plugin_id>
      tcLog.testSuiteStarted(pluginId).use {

        pidToResults.value.groupBy { it.pluginDescriptor.version }.forEach { versionToResults ->
          val version = versionToResults.key

          //multiple plugins might have the same pluginIds and the same versions
          versionToResults.value.forEach { result ->

            val testName = genTestName(result.pluginDescriptor, result.ideDescriptor.ideVersion, lastUpdates)

            tcLog.testStarted(testName).use {
              when (result) {
                is VResult.Nice -> {/*test is passed.*/
                }
                is VResult.Problems -> {

                  val overview = StringBuilder()
                  overview.append("Plugin URL: $pluginLink").append('\n')

                  val problems = result.problems

                  overview.append("$pluginId:$version has ${problems.keySet().size} ${"problem".pluralize(problems.keySet().size)}").append('\n')

                  val missingNonOptionals = result.dependenciesGraph.getMissingNonOptionalDependencies()
                  if (missingNonOptionals.isNotEmpty()) {
                    overview.append("Some problems might be caused by missing plugins:").append('\n')
                    missingNonOptionals.forEach {
                      overview.append("    $it").append('\n')
                    }
                  }

                  val missingOptionals = result.dependenciesGraph.getMissingOptionalDependencies().filterKeys { !options.ignoreMissingOptionalDependency(it) }
                  if (missingOptionals.isNotEmpty()) {
                    missingOptionals.forEach {
                      val pluginOrModule = if (it.key.id.startsWith(INTELLIJ_MODULES_PREFIX)) "module" else "plugin"
                      overview.append("Missing optional $pluginOrModule ${it.key.id}: ${it.value.reason}").append('\n')
                    }
                  }

                  val problemsContent: String

                  val notFoundClassesProblems = problems.keySet().filterIsInstance<ClassNotFoundProblem>()
                  if (missingNonOptionals.isNotEmpty() && notFoundClassesProblems.size > 20) {
                    //probably these all problems are caused by missing plugin dependencies.

                    val otherProblems: String = problems.asMap()
                        .filterKeys { it !in notFoundClassesProblems }
                        .entries.sortedBy { it.key.javaClass.name }
                        .joinToString(separator = "\n") {
                          "#${it.key.getDescription()}\n" +
                              it.value.joinToString(separator = "\n") { "    at $it" }
                        }

                    problemsContent = "There are too much missing classes (${notFoundClassesProblems.size});\n" +
                        "it's probably because of missing plugins (${missingNonOptionals.map { it.missing.id }});\n" +
                        "the following classes are not found: [${notFoundClassesProblems.map { it.unknownClass }.joinToString()}];\n" +
                        otherProblems

                  } else {
                    problemsContent = problems.asMap().entries.sortedBy { it.key.javaClass.name }.joinToString(separator = "\n") {
                      "#${it.key.getDescription()}\n" +
                          it.value.joinToString(separator = "\n") { "    at $it" }
                    }
                  }

                  tcLog.testStdErr(testName, problemsContent)

                  tcLog.testFailed(testName, overview.toString(), "")
                }
                is VResult.BadPlugin -> {
                  tcLog.testStdErr(testName, "Plugin is invalid: ${result.reason}")

                  tcLog.testFailed(testName, "Plugin is invalid: ${result.reason}", "")
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

  private fun requestLatestVersionsOfUpdatesForEachCheckedIde(ideVersions: List<IdeVersion>): Map<IdeVersion, List<UpdateInfo>> =
      ideVersions.associate { ideVersion ->
        val lastCompatibleUpdates: List<UpdateInfo> = try {
          val repository: PluginRepository = pluginRepository
          repository.getLastCompatibleUpdates(ideVersion)
        } catch(e: Exception) {
          LOG.warn("Unable to connect to Plugins Repository to get latest versions of plugins for $ideVersion", e)
          emptyList()
        }
        ideVersion to lastCompatibleUpdates.sortedByDescending { it.updateId }.distinctBy { it.pluginId }
      }

  private fun String.pluralize(times: Int): String {
    if (times < 0) throw IllegalArgumentException("Negative value")
    if (times == 0) return ""
    if (times == 1) return this else return this + "s"
  }

  private fun genTestName(pluginDescriptor: PluginDescriptor,
                          ideVersion: IdeVersion,
                          lastUpdates: Map<IdeVersion, List<UpdateInfo>>): String {
    val simple = "(${pluginDescriptor.version})"
    val relevant = lastUpdates[ideVersion] ?: return simple
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
        val type = if (missingToProblems.key.pluginId.startsWith(INTELLIJ_MODULES_PREFIX)) "module" else "plugin"
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
    //missing dependencies
    //....(missing#1)
    //.........Required for plugin1, plugin2, plugin3
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

    results.results.filterIsInstance<VResult.Problems>()
        .flatMap { descr -> descr.dependenciesGraph.getMissingNonOptionalDependencies().map { it.missing.id to descr.pluginDescriptor } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.isNotEmpty() }
        .apply {
          if (this.isNotEmpty()) {
            tcLog.testSuiteStarted("(missing dependencies)").use {
              this.forEach { entry ->
                val testName = "(${entry.key})"
                tcLog.testStarted(testName).use {
                  tcLog.testFailed(testName, "Required for ${entry.value.joinToString()}", "")
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