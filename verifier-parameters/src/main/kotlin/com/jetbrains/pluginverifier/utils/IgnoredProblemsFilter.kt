package com.jetbrains.pluginverifier.utils

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.Plugin
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.problems.Problem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.regex.Pattern

class IgnoredProblemsFilter(val problemsToIgnore: Multimap<Pair<String, String>, Pattern> = ImmutableMultimap.of(),
                            val saveIgnoredProblemsFile: File?) : ProblemsFilter {

  private val LOG: Logger = LoggerFactory.getLogger(IgnoredProblemsFilter::class.java)

  override fun isRelevantProblem(plugin: Plugin, problem: Problem): Boolean = !isIgnoredProblem(plugin, problem)

  private fun isIgnoredProblem(plugin: Plugin, problem: Problem): Boolean {
    val xmlId = plugin.pluginId
    val version = plugin.pluginVersion
    for ((key, ignoredPattern) in problemsToIgnore.entries()) {
      val ignoreXmlId = key.first
      val ignoreVersion = key.second

      if (xmlId == ignoreXmlId) {
        if (ignoreVersion.isEmpty() || version == ignoreVersion) {
          val regex = ignoredPattern.toRegex()
          if (problem.getShortDescription().matches(regex)) {
            appendToIgnoredProblemsFileOrLog(plugin, problem, regex)
            return true
          }
        }
      }
    }
    return false
  }

  private fun appendToIgnoredProblemsFileOrLog(plugin: Plugin, problem: Problem, regex: Regex) {
    val ap = "Problem of the plugin $plugin was ignored by the ignoring pattern: ${regex.pattern}:\n" +
        "#" + problem.getShortDescription()

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