package com.jetbrains.structure.plugin

sealed class PluginCreationResult<out PluginType : Plugin> {
  abstract val plugin: PluginType?
}

data class PluginCreationFail<out PluginType : Plugin>(val errorsAndWarnings: List<PluginProblem>) : PluginCreationResult<PluginType>() {
  override val plugin: PluginType? = null

  override fun toString(): String = "Failed: ${errorsAndWarnings.joinToString()}"
}

data class PluginCreationSuccess<out PluginType : Plugin>(override val plugin: PluginType, val warnings: List<PluginProblem>) :
    PluginCreationResult<PluginType>() {
  override fun toString(): String = "Success" + (if (warnings.isNotEmpty()) " but warnings: " + warnings.joinToString() else "")
}