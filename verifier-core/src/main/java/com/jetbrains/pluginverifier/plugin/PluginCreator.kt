package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.File

interface PluginCreator {
  fun createPlugin(pluginCoordinate: PluginCoordinate): CreatePluginResult

  fun createPluginByFile(pluginFile: File): CreatePluginResult

  fun createPluginByFileLock(pluginFileLock: FileLock): CreatePluginResult

  fun createResultByExistingPlugin(plugin: IdePlugin): CreatePluginResult
}