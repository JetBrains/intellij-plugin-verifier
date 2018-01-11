package com.jetbrains.plugin.structure.dotnet.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

object InvalidIdError : PluginProblem() {
  override val level = Level.ERROR
  override val message = "The id parameter in metadata must consist of two parts (company and a plugin name) separated by dot"
}