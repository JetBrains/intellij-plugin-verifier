package com.intellij.structure.plugin

import com.intellij.structure.problems.PluginProblem

sealed class PluginCreationResult {
  abstract val isSuccess: Boolean
}

data class PluginCreationFail(val errorsAndWarnings: List<PluginProblem>) : PluginCreationResult() {
  override val isSuccess: Boolean = false

  override fun toString(): String = "Failed: ${errorsAndWarnings.joinToString()}"
}

data class PluginCreationSuccess(val plugin: Plugin, val warnings: List<PluginProblem>) : PluginCreationResult() {
  override val isSuccess: Boolean = true

  override fun toString(): String = "Success" + (if (warnings.isNotEmpty()) " but warnings: " + warnings.joinToString() else "")
}