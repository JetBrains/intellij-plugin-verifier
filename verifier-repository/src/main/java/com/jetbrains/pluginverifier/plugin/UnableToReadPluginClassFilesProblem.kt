package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class UnableToReadPluginClassFilesProblem : PluginProblem() {
  override val level = Level.ERROR

  override val message
    get() = "Unable to read plugin class files"
}