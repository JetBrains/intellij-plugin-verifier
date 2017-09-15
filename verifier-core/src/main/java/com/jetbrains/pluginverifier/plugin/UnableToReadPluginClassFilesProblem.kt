package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

object UnableToReadPluginClassFilesProblem : PluginProblem() {
  override val level: Level = Level.ERROR

  override val message: String = "Unable to read plugin class files"
}