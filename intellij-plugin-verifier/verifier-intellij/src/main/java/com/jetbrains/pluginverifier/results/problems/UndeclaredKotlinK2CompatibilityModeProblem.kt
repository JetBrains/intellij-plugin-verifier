package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.intellij.problems.UndeclaredKotlinK2CompatibilityMode

data class UndeclaredKotlinK2CompatibilityModeProblem(private val problem: UndeclaredKotlinK2CompatibilityMode) : CompatibilityProblem() {
  override val problemType = "Plugin descriptor problem"
  override val shortDescription = "Plugin does not declare Kotlin mode in the <supportsKotlinPluginMode> extension."
  override val fullDescription = problem.detailedMessage
}