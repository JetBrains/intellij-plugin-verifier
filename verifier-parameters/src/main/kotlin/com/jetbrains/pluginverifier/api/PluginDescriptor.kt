package com.jetbrains.pluginverifier.api

import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.FileLock

/**
 * Descriptor of the plugin to be checked
 */
sealed class PluginDescriptor(val pluginId: String, val version: String) {

  class ByUpdateInfo(val updateInfo: UpdateInfo) : PluginDescriptor(updateInfo.pluginId, updateInfo.version)

  class ByFileLock(pluginId: String, version: String, val fileLock: FileLock) : PluginDescriptor(pluginId, version)

  class ByInstance(val plugin: Plugin, val resolver: Resolver) : PluginDescriptor(plugin.pluginId ?: "", plugin.pluginVersion ?: "")
}