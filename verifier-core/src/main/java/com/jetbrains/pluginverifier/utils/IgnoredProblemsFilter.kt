package com.jetbrains.pluginverifier.utils

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.tasks.PluginIdAndVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.regex.Pattern

class IgnoredProblemsFilter(val problemsToIgnore: Multimap<PluginIdAndVersion, Pattern> = ImmutableMultimap.of(),
                            val saveIgnoredProblemsFile: File?) : ProblemsFilter {

  private val LOG: Logger = LoggerFactory.getLogger(IgnoredProblemsFilter::class.java)

  override fun isRelevantProblem(plugin: IdePlugin, problem: Problem): Boolean = !isIgnoredProblem(plugin, problem)

  private fun isIgnoredProblem(plugin: IdePlugin, problem: Problem): Boolean {
    val xmlId = plugin.pluginId
    val version = plugin.pluginVersion
    for ((key, ignoredPattern) in problemsToIgnore.entries()) {
      val (ignoreXmlId, ignoreVersion) = key

      if (xmlId == ignoreXmlId) {
        if (ignoreVersion.isEmpty() || version == ignoreVersion) {
          val regex = ignoredPattern.toRegex()
          if (problem.shortDescription.matches(regex)) {
            appendToIgnoredProblemsFileOrLog(plugin, problem, regex)
            return true
          }
        }
      }
    }
    return false
  }

  private fun appendToIgnoredProblemsFileOrLog(plugin: IdePlugin, problem: Problem, regex: Regex) {
    val ap = "Problem of the plugin $plugin was ignored by the ignoring pattern: ${regex.pattern}:\n" +
        "#" + problem.shortDescription

    if (saveIgnoredProblemsFile != null) {
      try {
        saveIgnoredProblemsFile.appendText(ap)
      } catch (e: Exception) {
        LOG.error("Unable to append the ignored problem to file $saveIgnoredProblemsFile", e)
      }
    } else {
      LOG.info(ap)
    }
  }
}