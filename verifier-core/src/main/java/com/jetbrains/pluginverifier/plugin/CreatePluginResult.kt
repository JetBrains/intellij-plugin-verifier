package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

sealed class CreatePluginResult : Closeable {
  data class OK internal constructor(val plugin: IdePlugin,
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