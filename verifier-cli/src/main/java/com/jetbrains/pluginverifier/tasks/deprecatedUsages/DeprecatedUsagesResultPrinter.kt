package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.deprecated.formatUsageLocation
import com.jetbrains.pluginverifier.results.deprecated.locationType
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * @author Sergey Patrikeev
 */
class DeprecatedUsagesResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    val deprecatedUsagesResult = taskResult as DeprecatedUsagesResult
    if (outputOptions.needTeamCityLog) {
      val teamCityLog = TeamCityLog(System.out)
      with(deprecatedUsagesResult) {
        val deprecatedIdeApiToPluginUsages = hashMapOf<Location, MutableMap<PluginInfo, Int>>()
        for ((plugin, pluginUsages) in pluginDeprecatedUsages) {
          pluginUsages
              .asSequence()
              .filter { it.deprecatedElement in deprecatedIdeApiElements }
              .forEach {
                deprecatedIdeApiToPluginUsages
                    .getOrPut(it.deprecatedElement, { hashMapOf() })
                    .compute(plugin, { _, c -> (c ?: 0) + 1 })
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
          val testName = "(Mostly used $ideVersion deprecated API)"
          teamCityLog.testStarted(testName).use {
            val sortedByNumberOfPlugins = deprecatedIdeApiToPluginUsages.toList().sortedByDescending { it.second.size }
            val fullTestMessage = buildString {
              for ((deprecatedApiElement, pluginToUsagesNumber) in sortedByNumberOfPlugins) {
                append(deprecatedApiElement.locationType.capitalize())
                append(" " + deprecatedApiElement.formatUsageLocation())
                appendln(" is used in ${pluginToUsagesNumber.size} " + "plugin".pluralize(pluginToUsagesNumber.size) + ":")
                for ((plugin, usagesNumber) in pluginToUsagesNumber.toList().sortedByDescending { it.second }.take(10)) {
                  append("  ")
                  append("${plugin.pluginId} ${plugin.version}")
                  appendln(" ($usagesNumber " + "usage".pluralize(usagesNumber) + ")")
                }
                appendln()
              }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "There are ${deprecatedIdeApiToPluginUsages.size} deprecated API " + "element".pluralize(deprecatedIdeApiToPluginUsages.size) + " used in some plugins", "")
          }
        }

        /**
         * Print the "Unused IU-172.1331 deprecated API" tab like this:
         * There are 2 externally unused deprecated API classes in IU-172.1331:
         *   Class org.jetbrains.Unused
         *   Class org.jetbrains.SuperOldClass
         *
         * There are 1 externally unused deprecated API method in IU-172.1331:
         *   Method org.jetbrains.Unused.unusedMethod
         */
        val unusedIdeDeprecatedElements = deprecatedIdeApiElements - deprecatedIdeApiToPluginUsages.keys
        if (unusedIdeDeprecatedElements.isNotEmpty()) {
          val testName = "(Unused $ideVersion deprecated API elements)"
          teamCityLog.testStarted(testName).use {
            val fullTestMessage = buildString {
              for ((locationType, unusedApiElementsWithType) in unusedIdeDeprecatedElements.groupBy { it.locationType }) {
                appendln("There are " + unusedApiElementsWithType.size + " externally unused deprecated API " + locationType.pluralize(unusedApiElementsWithType.size) + " in $ideVersion:")
                val formattedUnusedUsages = unusedApiElementsWithType.map { it.formatUsageLocation() }.sorted()
                for (unusedElement in formattedUnusedUsages) {
                  append("  ")
                  appendln(unusedElement)
                }
                appendln()
              }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "There are ${unusedIdeDeprecatedElements.size} deprecated API " + "element".pluralize(unusedIdeDeprecatedElements.size) + " in $ideVersion", "")
          }
        }

        val allPluginsHavingDeprecatedApiUsages = deprecatedIdeApiToPluginUsages.values.flatMap { it.keys }.distinct()
        val numberOfPlugins = allPluginsHavingDeprecatedApiUsages.size
        /**
         * Print the "List of 2 plugins being checked"
         *   org.plugin.one 1.0
         *   com.plugin.two 2.0
         */
        if (allPluginsHavingDeprecatedApiUsages.isNotEmpty()) {
          val testName = "(List of $numberOfPlugins " + "plugin".pluralize(numberOfPlugins) + " being checked)"
          teamCityLog.testStarted(testName).use {
            val fullTestMessage = buildString {
              allPluginsHavingDeprecatedApiUsages
                  .sortedBy { it.pluginId }
                  .forEach {
                    appendln("${it.pluginId} ${it.version}")
                  }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "We have checked $numberOfPlugins " + "plugin".pluralize(numberOfPlugins), "")
          }
        }

        if (deprecatedIdeApiToPluginUsages.isNotEmpty() || unusedIdeDeprecatedElements.isNotEmpty()) {
          teamCityLog.buildStatusFailure(
              buildString {
                append("In $ideVersion found ${deprecatedIdeApiElements.size} deprecated API " + "element".pluralize(deprecatedIdeApiElements.size) + ": ")
                append("${deprecatedIdeApiToPluginUsages.size} " + " are used in $allPluginsHavingDeprecatedApiUsages checked " + "plugin".pluralize(numberOfPlugins))
                append(" and ${unusedIdeDeprecatedElements.size} are unused in these plugins")
              }
          )
        } else {
          teamCityLog.buildStatusSuccess("API of $ideVersion is up to date with $allPluginsHavingDeprecatedApiUsages " + "plugin".pluralize(numberOfPlugins) + " being checked")
        }
      }

    }
  }

}