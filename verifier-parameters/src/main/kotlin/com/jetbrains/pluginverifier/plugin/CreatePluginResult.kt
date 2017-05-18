package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

sealed class CreatePluginResult : Closeable {
  data class OK(val pluginFileLock: FileLock?,
                val pluginCreationSuccess: PluginCreationSuccess,
                val resolver: Resolver) : CreatePluginResult() {
    override fun close() {
      try {
        resolver.closeLogged()
      } finally {
        pluginFileLock?.release()
      }
    }
  }

  data class BadPlugin(val pluginCreationFail: PluginCreationFail) : CreatePluginResult() {
    override fun close() = Unit
  }

  data class NotFound(val reason: String) : CreatePluginResult() {
    override fun close() = Unit
  }
}