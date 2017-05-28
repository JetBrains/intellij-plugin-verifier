package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.plugin.Plugin
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import java.io.Closeable

sealed class CreatePluginResult : Closeable {
  data class OK internal constructor(val plugin: Plugin, val warnings: List<PluginProblem>, val resolver: Resolver) : CreatePluginResult() {
    override fun close() = resolver.close()
  }

  data class BadPlugin internal constructor(val pluginErrorsAndWarnings: List<PluginProblem>) : CreatePluginResult() {
    override fun close() = Unit
  }

  data class NotFound internal constructor(val reason: String) : CreatePluginResult() {
    override fun close() = Unit
  }
}