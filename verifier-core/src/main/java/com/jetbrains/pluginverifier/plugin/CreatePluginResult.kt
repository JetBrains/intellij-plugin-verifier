package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.plugin.Plugin
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

sealed class CreatePluginResult : Closeable {
  data class OK internal constructor(val plugin: Plugin,
                                     val warnings: List<PluginProblem>,
                                     val resolver: Resolver,
                                     private val pluginLock: FileLock) : CreatePluginResult() {
    override fun close() {
      pluginLock.close()
      resolver.close()
    }
  }

  data class BadPlugin internal constructor(val pluginErrorsAndWarnings: List<PluginProblem>) : CreatePluginResult() {
    override fun close() = Unit
  }

  data class NotFound internal constructor(val reason: String) : CreatePluginResult() {
    override fun close() = Unit
  }
}