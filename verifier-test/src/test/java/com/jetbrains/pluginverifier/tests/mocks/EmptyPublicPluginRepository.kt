package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * Created by Sergey.Patrikeev
 */
object EmptyPublicPluginRepository : PluginRepository {
  override fun getAllPlugins(): List<PluginInfo> = emptyList()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = emptyList()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = null

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> = emptyList()

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> = emptyList()

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = null

}