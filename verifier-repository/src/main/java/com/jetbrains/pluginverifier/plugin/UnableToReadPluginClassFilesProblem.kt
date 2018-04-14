package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

//todo: provide clear message.
data class UnableToReadPluginClassFilesProblem(val exception: Throwable) : PluginProblem() {
  override val level = Level.ERROR

  override val message = "Unable to read plugin class files: ${exception.message}"
}