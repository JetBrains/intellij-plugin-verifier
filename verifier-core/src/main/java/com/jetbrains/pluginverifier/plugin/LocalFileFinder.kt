package com.jetbrains.pluginverifier.plugin

import java.io.File

class LocalFileFinder(private val pluginFile: File) : PluginFileFinder {
  override fun findPluginFile(): PluginFileFinder.Result = PluginFileFinder.Result.Found(IdleFileLock(pluginFile))
}