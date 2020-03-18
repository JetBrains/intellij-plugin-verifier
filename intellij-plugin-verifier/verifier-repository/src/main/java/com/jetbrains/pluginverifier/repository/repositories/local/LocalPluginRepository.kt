package com.jetbrains.pluginverifier.repository.repositories.local

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.VERSION_COMPARATOR

/**
 * [PluginRepository] consisting of [locally] [LocalPluginInfo] stored plugins.
 */
class LocalPluginRepository(private val plugins: MutableList<LocalPluginInfo> = arrayListOf()) : PluginRepository {

  fun addLocalPlugin(idePlugin: IdePlugin): LocalPluginInfo {
    val localPluginInfo = LocalPluginInfo(idePlugin)
    plugins.add(localPluginInfo)
    return localPluginInfo
  }

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
    plugins.filter { it.isCompatibleWith(ideVersion) }
      .groupBy { it.pluginId }
      .mapValues { it.value.maxWith(VERSION_COMPARATOR)!! }
      .values.toList()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
    getAllVersionsOfPlugin(pluginId).filter { it.isCompatibleWith(ideVersion) }.maxWith(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
    plugins.filter { it.pluginId == pluginId }

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
    plugins.find { moduleId in it.definedModules }?.pluginId

  override val presentableName
    get() = "Local Plugin Repository"

  override fun toString() = presentableName

}