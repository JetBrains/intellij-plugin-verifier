package com.jetbrains.pluginverifier.filter

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.problems.Problem
import java.io.Closeable

abstract class ProblemsFilter : Closeable {
  abstract fun accept(plugin: IdePlugin, problem: Problem): Boolean

  protected abstract fun onClose()

  override fun close() = onClose()
}