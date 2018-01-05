package com.jetbrains.pluginverifier.parameters.filtering

import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

class IgnoredProblemsFilter(private val problemsToIgnore: Multimap<PluginIdAndVersion, Regex>) : ProblemsFilter {

  override fun shouldReportProblem(plugin: IdePlugin, ideVersion: IdeVersion, problem: Problem, verificationContext: VerificationContext): ProblemsFilter.Result {
    val xmlId = plugin.pluginId
    val version = plugin.pluginVersion
    for ((pluginIdAndVersion, ignoredPattern) in problemsToIgnore.entries()) {
      val ignoreXmlId = pluginIdAndVersion.pluginId
      val ignoreVersion = pluginIdAndVersion.version

      if (xmlId == ignoreXmlId) {
        if (ignoreVersion.isEmpty() || version == ignoreVersion) {
          if (problem.shortDescription.matches(ignoredPattern)) {
            return ProblemsFilter.Result.Ignore("ignoring pattern matches - $ignoredPattern")
          }
        }
      }
    }
    return ProblemsFilter.Result.Report
  }

}