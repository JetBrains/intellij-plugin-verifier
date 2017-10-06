package com.jetbrains.pluginverifier.parameters.filtering

import com.google.common.collect.Multimap
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.problems.Problem
import java.io.File

class IgnoredProblemsFilter(private val problemsToIgnore: Multimap<PluginIdAndVersion, Regex>,
                            private val saveIgnoredProblemsFile: File?) : ProblemsFilter() {

  private val ignoredProblems = arrayListOf<Triple<IdePlugin, Problem, Regex>>()

  override fun accept(plugin: IdePlugin, problem: Problem): Boolean = !isIgnoredProblem(plugin, problem)

  private fun isIgnoredProblem(plugin: IdePlugin, problem: Problem): Boolean {
    val xmlId = plugin.pluginId
    val version = plugin.pluginVersion
    for ((pluginIdAndVersion, ignoredPattern) in problemsToIgnore.entries()) {
      val (ignoreXmlId, ignoreVersion) = pluginIdAndVersion

      if (xmlId == ignoreXmlId) {
        if (ignoreVersion.isEmpty() || version == ignoreVersion) {
          if (problem.shortDescription.matches(ignoredPattern)) {
            ignoredProblems.add(Triple(plugin, problem, ignoredPattern))
            return true
          }
        }
      }
    }
    return false
  }

  private fun generateIgnoredProblemLine(plugin: IdePlugin, problem: Problem, regex: Regex): String =
      "Problem of the plugin $plugin was ignored by the ignoring pattern: ${regex.pattern}:\n#${problem.fullDescription}"

  override fun onClose() {
    saveIgnoredProblemsFile?.bufferedWriter()?.use { writer ->
      for ((plugin, problem, regex) in ignoredProblems) {
        writer.appendln(generateIgnoredProblemLine(plugin, problem, regex))
      }
    }
  }

}