package com.jetbrains.pluginverifier.plugin

fun PluginCoordinate.create(pluginCreator: PluginCreator): CreatePluginResult {
  val pluginFile = fileFinder.findPluginFile()
  return when (pluginFile) {
    is PluginFileFinder.Result.Found -> pluginCreator.createPluginByFileLock(pluginFile.pluginFileLock)
    is PluginFileFinder.Result.FailedToDownload -> CreatePluginResult.FailedToDownload(pluginFile.reason)
    is PluginFileFinder.Result.NotFound -> CreatePluginResult.NotFound(pluginFile.reason)
  }
}
