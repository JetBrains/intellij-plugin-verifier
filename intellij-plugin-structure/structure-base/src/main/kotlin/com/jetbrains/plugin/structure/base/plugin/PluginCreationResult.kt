package com.jetbrains.plugin.structure.base.plugin

sealed class PluginCreationResult<out PluginType : Plugin>

data class PluginCreationFail<out PluginType : Plugin>(val errorsAndWarnings: List<PluginProblem>) : PluginCreationResult<PluginType>() {
  constructor(error: PluginProblem) : this(listOf(error))

  override fun toString(): String = "Failed: ${errorsAndWarnings.joinToString()}"
}

data class PluginCreationSuccess<out PluginType : Plugin>(val plugin: PluginType, val warnings: List<PluginProblem>) :
  PluginCreationResult<PluginType>() {
  override fun toString(): String = "Success" + (if (warnings.isNotEmpty()) " but warnings: " + warnings.joinToString() else "")
}