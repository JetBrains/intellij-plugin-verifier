package com.jetbrains.pluginverifier.repository.repositories.custom

import com.google.common.base.Suppliers
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.VERSION_COMPARATOR
import java.util.concurrent.TimeUnit

/**
 * Base class for all repositories configured for a specific plugin.
 * For example, there are the following plugins configured specially:
 * 1) MultiPush plugin
 * 2) ExceptionAnalyzer plugin
 * 3) TeamCity IDEA plugin
 * 4) Upsource plugin
 */
abstract class CustomPluginRepository : PluginRepository {

  private val allPluginsCache = Suppliers.memoizeWithExpiration({ requestAllPlugins() }, 1, TimeUnit.MINUTES)

  protected abstract fun requestAllPlugins(): List<CustomPluginInfo>

  override fun getAllPlugins(): List<CustomPluginInfo> = allPluginsCache.get()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      getAllPlugins()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllPlugins()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllPlugins().maxWith(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
      getAllPlugins().filter { it.pluginId == pluginId }

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = null

}