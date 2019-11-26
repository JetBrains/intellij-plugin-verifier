package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

/**
 * Represents API of the plugin repository.
 */
interface PluginRepository {

  /**
   * Returns the latest plugins' versions compatible with [ideVersion].
   */
  fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo>

  /**
   * Returns the last version of the plugin with ID equal to [pluginId]
   * compatible with [ideVersion].
   */
  fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo?

  /**
   * Returns all versions of the plugin with ID equal to [pluginId].
   */
  fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo>

  /**
   * Given the [moduleId] returns the ID of the plugin that
   * declares this module.
   */
  fun getIdOfPluginDeclaringModule(moduleId: String): String?

}