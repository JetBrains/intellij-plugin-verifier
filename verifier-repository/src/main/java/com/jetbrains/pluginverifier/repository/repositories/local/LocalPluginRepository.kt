package com.jetbrains.pluginverifier.repository.repositories.local

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.VERSION_COMPARATOR

/**
 * [PluginRepository] consisting of [locally] [LocalPluginInfo] stored plugins.
 */
class LocalPluginRepository(
    private val plugins: MutableList<LocalPluginInfo> = arrayListOf()
) : PluginRepository {

  fun addLocalPlugin(idePlugin: IdePlugin): LocalPluginInfo {
    val localPluginInfo = LocalPluginInfo(idePlugin)
    plugins.add(localPluginInfo)
    return localPluginInfo
  }

  override fun getAllPlugins() = plugins

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      plugins.filter { it.isCompatibleWith(ideVersion) }
          .groupBy { it.pluginId }
          .mapValues { it.value.maxWith(VERSION_COMPARATOR)!! }
          .values.toList()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      plugins.filter { it.isCompatibleWith(ideVersion) && it.pluginId == pluginId }

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId).maxWith(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
      plugins.filter { it.pluginId == pluginId }

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
      plugins.find { moduleId in it.definedModules }?.pluginId

  fun findPluginById(pluginId: String): LocalPluginInfo? = plugins.find { it.pluginId == pluginId }

  fun findPluginByModule(moduleId: String): LocalPluginInfo? = plugins.find { moduleId in it.definedModules }

  override fun toString() = "Local Plugin Repository"

}