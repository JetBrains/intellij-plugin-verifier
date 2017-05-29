package com.intellij.structure.plugin

import com.intellij.structure.problems.PluginProblem

sealed class PluginCreationResult

data class PluginCreationFail(val errorsAndWarnings: List<PluginProblem>) : PluginCreationResult() {
  override fun toString(): String = "Failed: ${errorsAndWarnings.joinToString()}"
}

data class PluginCreationSuccess(val plugin: Plugin, val warnings: List<PluginProblem>) : PluginCreationResult() {
  override fun toString(): String = "Success" + (if (warnings.isNotEmpty()) " but warnings: " + warnings.joinToString() else "")
}