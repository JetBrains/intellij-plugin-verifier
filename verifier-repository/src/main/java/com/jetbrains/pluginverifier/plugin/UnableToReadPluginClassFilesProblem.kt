package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

data class UnableToReadPluginClassFilesProblem(val exception: Throwable) : PluginProblem() {
  override val level: Level = Level.ERROR

  override val message: String = "Unable to read plugin class files: ${exception.message}"
}