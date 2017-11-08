package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

data class LocalPluginRepository(val ideVersion: IdeVersion,
                                 private val plugins: List<LocalPluginInfo>) {
  fun findPluginById(pluginId: String): LocalPluginInfo? = plugins.find { it.pluginId == pluginId }

  fun findPluginByModule(moduleId: String): LocalPluginInfo? = plugins.find { moduleId in it.definedModules }
}