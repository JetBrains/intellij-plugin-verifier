package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.classes.locator.ClassLocationsContainer
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

sealed class CreatePluginResult : Closeable {
  data class OK(val plugin: IdePlugin,
                val warnings: List<PluginProblem>,
                val locationsContainer: ClassLocationsContainer,
                private val pluginLock: FileLock) : CreatePluginResult() {
    override fun close() {
      pluginLock.close()
      locationsContainer.close()
    }
  }

  data class BadPlugin(val pluginErrorsAndWarnings: List<PluginProblem>) : CreatePluginResult() {
    override fun close() = Unit
  }

  data class FailedToDownload(val reason: String) : CreatePluginResult() {
    override fun close() = Unit
  }

  data class NotFound(val reason: String) : CreatePluginResult() {
    override fun close() = Unit
  }
}