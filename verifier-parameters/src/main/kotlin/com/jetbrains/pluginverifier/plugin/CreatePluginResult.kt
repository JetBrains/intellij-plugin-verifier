package com.jetbrains.pluginverifier.plugin

import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.resolvers.Resolver
import java.io.Closeable

sealed class CreatePluginResult : Closeable {
  data class OK internal constructor(val success: PluginCreationSuccess, val resolver: Resolver) : CreatePluginResult() {
    override fun close() = resolver.close()
  }

  data class BadPlugin internal constructor(val pluginCreationFail: PluginCreationFail) : CreatePluginResult() {
    override fun close() = Unit
  }

  data class NotFound internal constructor(val reason: String) : CreatePluginResult() {
    override fun close() = Unit
  }
}