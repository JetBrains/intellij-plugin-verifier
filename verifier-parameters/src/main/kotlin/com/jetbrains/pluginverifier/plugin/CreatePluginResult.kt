package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

sealed class CreatePluginResult : Closeable {
  data class OK(val success: PluginCreationSuccess,
                val resolver: Resolver,
                val pluginLock: FileLock?) : CreatePluginResult() {
    override fun close() {
      try {
        resolver.close()
      } finally {
        pluginLock?.release()
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