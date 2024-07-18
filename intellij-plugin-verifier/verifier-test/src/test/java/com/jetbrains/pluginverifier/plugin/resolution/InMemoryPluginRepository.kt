package com.jetbrains.pluginverifier.plugin.resolution

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository

class InMemoryPluginRepository : PluginRepository{
  override val presentableName: String = "In-Memory Plugin Repository"

  private val plugins = mutableListOf<PluginInfo>()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> {
    return plugins.toList()
  }

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo? {
    return plugins.lastOrNull { it.pluginId == pluginId }
  }

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> {
    return plugins.filter { it.pluginId == pluginId }
  }

  override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?): List<PluginInfo> {
    throw UnsupportedOperationException("Not implemented")
  }

  operator fun plusAssign(pluginInfo: PluginInfo) {
    plugins += pluginInfo
  }

  companion object {
    fun create(vararg plugins: PluginInfo): InMemoryPluginRepository {
      return InMemoryPluginRepository().apply {
        plugins.forEach {
          this += it
        }
      }
    }
  }
}