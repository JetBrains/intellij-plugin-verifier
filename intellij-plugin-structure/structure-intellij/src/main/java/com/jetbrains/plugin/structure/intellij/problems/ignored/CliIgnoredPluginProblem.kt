package com.jetbrains.plugin.structure.intellij.problems.ignored

import com.jetbrains.plugin.structure.base.problems.PluginProblems
import kotlin.reflect.KClass

data class CliIgnoredPluginProblem(val id: String, val pluginProblemClassFqn: String, val since: String) {
  val pluginProblemClass: KClass<*>?
    get() {
      return PluginProblems.resolveClass(pluginProblemClassFqn)
    }
}

