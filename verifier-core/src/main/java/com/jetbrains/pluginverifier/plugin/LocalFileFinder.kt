package com.jetbrains.pluginverifier.plugin

import java.nio.file.Path

class LocalFileFinder(private val pluginFile: Path) : PluginFileFinder {
  override fun findPluginFile(): PluginFileFinder.Result = PluginFileFinder.Result.Found(IdleFileLock(pluginFile))
}