package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.repository.Browseable
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.usage.formatUsageLocation
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

class DeprecatedUsagesResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  private companion object {
    const val LIMIT_OF_MOST_USING_PLUGINS = 10
  }

  private val Location.locationType: String
    get() = when (this) {
      is ClassLocation -> "class"
      is MethodLocation -> "method"
      is FieldLocation -> "field"
    }

  override fun printResults(taskResult: TaskResult) {
    val deprecatedUsagesResult = taskResult as DeprecatedUsagesResult
    if (outputOptions.needTeamCityLog) {
      val teamCityLog = TeamCityLog(System.out)
      with(deprecatedUsagesResult) {
        val deprecatedIdeApiToPluginUsages = hashMapOf<Location, MutableMap<PluginInfo, Int>>()
        for ((plugin, pluginUsages) in pluginDeprecatedUsages) {
          pluginUsages
              .asSequence()
              .filter { it.apiElement in deprecatedIdeApiElements }
              .forEach {
                deprecatedIdeApiToPluginUsages
                    .getOrPut(it.apiElement) { hashMapOf() }
                    .compute(plugin) { _, c -> (c ?: 0) + 1 }
              }
        }

        /**
         * Print the "Mostly used IU-172.1331 deprecated API" tab like this:
         *   Class org.jetbrains.Old is used in 3 plugins:
         *     org.plugin.one:1.0 (3 usages)
         *     org.plugin.two:2.0 (2 usages)
         *     org.plugin.three:3.0 (1 usage)
         *     <limited to ten mostly using plugins>
         *
         *   Method org.jetbrains.Dep.oldMethod() : void is used in 2 plugins:
         *     org.plugin.one:1.0 (2 usages)
         *     org.plugin.two:2.0 (1 usage)
         */
        if (deprecatedIdeApiToPluginUsages.isNotEmpty()) {
          val testName = "(Mostly used $verifiedIdeVersion deprecated API)"
          teamCityLog.testStarted(testName).use {
            val sortedByNumberOfPlugins = deprecatedIdeApiToPluginUsages.toList()
                .sortedWith(compareByDescending<Pair<Location, MutableMap<PluginInfo, Int>>> { it.second.size }.thenBy { it.first.locationType })
            val fullTestMessage = buildString {
              for ((deprecatedApiElement, pluginToUsagesNumber) in sortedByNumberOfPlugins) {
                append(deprecatedApiElement.locationType.capitalize())
                append(" " + deprecatedApiElement.formatUsageLocation())
                append(" is used in ${pluginToUsagesNumber.size} " + "plugin".pluralize(pluginToUsagesNumber.size))
                val sortedByNumberOfUsages = pluginToUsagesNumber.toList()
                    .sortedWith(compareByDescending<Pair<PluginInfo, Int>> { it.second }.thenBy { it.first.pluginId })
                if (sortedByNumberOfUsages.size > LIMIT_OF_MOST_USING_PLUGINS) {
                  append(" (only the $LIMIT_OF_MOST_USING_PLUGINS most using plugins are shown)")
                }
                appendln()
                for ((plugin, usagesNumber) in sortedByNumberOfUsages.take(LIMIT_OF_MOST_USING_PLUGINS)) {
                  append("  ")
                  append("${plugin.pluginId} ${plugin.version}")
                  appendln(" ($usagesNumber " + "usage".pluralize(usagesNumber) + ")")
                }
                appendln()
              }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "There " + "is".pluralize(deprecatedIdeApiToPluginUsages.size) + " ${deprecatedIdeApiToPluginUsages.size} deprecated API " +
                "element".pluralize(deprecatedIdeApiToPluginUsages.size) + " in $verifiedIdeVersion used in the checked plugins.\n" +
                "You can explore concrete usages' details by looking into the verification-results " +
                "directory of a specific plugin (see the build artifacts), or via the Find External Usages action of the API Watcher plugin", "")
          }
        }

        /**
         * Print the "Unused IU-172.1331 deprecated API" tab like this:
         * There are 2 deprecated API classes in IU-172.1331 unused in the checked plugins:
         *   Class org.jetbrains.Unused
         *   Class org.jetbrains.SuperOldClass
         *
         * There is 1  deprecated API method in IU-172.1331:
         *   Method org.jetbrains.Unused.unusedMethod
         */
        val unusedIdeDeprecatedElements = deprecatedIdeApiElements - deprecatedIdeApiToPluginUsages.keys
        val unusedIdeApiElementsNumber = unusedIdeDeprecatedElements.size
        if (unusedIdeDeprecatedElements.isNotEmpty()) {
          val testName = "(Unused $verifiedIdeVersion deprecated API elements)"
          teamCityLog.testStarted(testName).use {
            val fullTestMessage = buildString {
              for ((locationType, unusedApiElementsWithType) in unusedIdeDeprecatedElements.groupBy { it.locationType }) {
                appendln("There " + "is".pluralize(unusedApiElementsWithType.size) + " ${unusedApiElementsWithType.size} " + "deprecated API " + locationType.pluralize(unusedApiElementsWithType.size) + " in $verifiedIdeVersion unused in checked plugins:")
                val formattedUnusedUsages = unusedApiElementsWithType.map { it.formatUsageLocation() }.sorted()
                for (unusedElement in formattedUnusedUsages) {
                  append("  ")
                  appendln(unusedElement)
                }
                appendln()
              }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "There " + "is".pluralize(unusedIdeApiElementsNumber) + " $unusedIdeApiElementsNumber deprecated API " +
                "element".pluralize(unusedIdeApiElementsNumber) + " in $verifiedIdeVersion unused in the checked plugins.", ""
            )
          }
        }

        val allCheckedPlugins = pluginDeprecatedUsages.keys
        val numberOfCheckedPlugins = allCheckedPlugins.size
        /**
         * Print the "List of 2 plugins being checked"
         *   org.plugin.one 1.0
         *   com.plugin.two 2.0
         */
        if (allCheckedPlugins.isNotEmpty()) {
          val testName = "(List of $numberOfCheckedPlugins " + "plugin".pluralize(numberOfCheckedPlugins) + " being checked)"
          teamCityLog.testStarted(testName).use {
            val fullTestMessage = buildString {
              allCheckedPlugins
                  .sortedBy { it.pluginId }
                  .forEach {
                    val overviewUrl = (it as? Browseable)?.browserUrl ?: ""
                    appendln("%-50s %-25s%s".format(it.pluginId, it.version, overviewUrl))
                  }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            val testMessage = buildString {
              appendln("There " + "is".pluralize(numberOfCheckedPlugins) + " $numberOfCheckedPlugins " + "plugin".pluralize(numberOfCheckedPlugins) + " checked.")
              append("The compatible " + "version".pluralize(numberOfCheckedPlugins) + " of the " + "plugin".pluralize(numberOfCheckedPlugins) + " " + "was".pluralize(numberOfCheckedPlugins) + " selected based on IDE version $ideVersionForCompatiblePlugins")
            }
            teamCityLog.testFailed(testName, testMessage, "")
          }
        }

        if (deprecatedIdeApiToPluginUsages.isNotEmpty() || unusedIdeDeprecatedElements.isNotEmpty()) {
          teamCityLog.buildStatusFailure(
              buildString {
                append("In $verifiedIdeVersion found ${deprecatedIdeApiElements.size} deprecated API " + "element".pluralize(deprecatedIdeApiElements.size) + ": ")
                append("${deprecatedIdeApiToPluginUsages.size} " + "is".pluralize(deprecatedIdeApiToPluginUsages.size) + " used in $numberOfCheckedPlugins checked " + "plugin".pluralize(numberOfCheckedPlugins))
                append(" and $unusedIdeApiElementsNumber " + "is".pluralize(unusedIdeApiElementsNumber) + " unused in " + "this".pluralize(numberOfCheckedPlugins) + " " + "plugin".pluralize(numberOfCheckedPlugins))
              }
          )
        } else {
          teamCityLog.buildStatusSuccess("API of $verifiedIdeVersion is up to date with $allCheckedPlugins " + "plugin".pluralize(numberOfCheckedPlugins) + " being checked")
        }
      }

    }
  }

}