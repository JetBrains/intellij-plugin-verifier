package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.FileLock

interface PluginCreator {
  fun createPluginByFileLock(pluginFileLock: FileLock): CreatePluginResult

  fun createByExistingPlugin(plugin: IdePlugin): CreatePluginResult
}