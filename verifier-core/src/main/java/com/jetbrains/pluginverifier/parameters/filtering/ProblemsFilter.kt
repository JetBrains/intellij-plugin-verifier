package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.results.problems.Problem
import java.io.Closeable

//todo: use reporter for ignored problems
abstract class ProblemsFilter : Closeable {
  abstract fun accept(plugin: IdePlugin, problem: Problem): Boolean

  protected abstract fun onClose()

  override fun close() = onClose()
}