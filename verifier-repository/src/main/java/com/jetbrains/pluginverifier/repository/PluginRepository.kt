package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult

/**
 * Represents API of the plugin repository.
 *
 * Its main implementation is [PublicPluginRepository]
 * that corresponds to the [JetBrains Plugins Repository](https://plugins.jetbrains.com/)
 */
interface PluginRepository {

  /**
   * Returns all plugins available in the repository.
   */
  fun getAllPlugins(): List<PluginInfo>

  /**
   * Returns all plugins compatible with this [ideVersion].
   */
  fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo>

  /**
   * Returns all versions of the plugin with ID equal to [pluginId]
   * compatible with [ideVersion].
   */
  fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo>

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
   * Downloads the file of the plugin by its [pluginInfo].
   */
  fun downloadPluginFile(pluginInfo: PluginInfo): FileRepositoryResult

  /**
   * Given the [update ID] [updateId], which is a unique identifier
   * of the plugin version in the Plugins Repository database,
   * returns its more detailed [UpdateInfo].
   */
  fun getPluginInfoById(updateId: Int): UpdateInfo?

  /**
   * Given the [moduleId] returns the ID of the plugin that
   * declares this module.
   */
  fun getIdOfPluginDeclaringModule(moduleId: String): String?

}