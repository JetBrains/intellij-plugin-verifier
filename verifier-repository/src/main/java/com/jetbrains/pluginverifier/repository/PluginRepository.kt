package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult

/**
 * Represents API of the plugin repository.
 *
 * Its main implementation is [PublicPluginRepository]
 * that corresponds to the [JetBrains Plugins Repository](https://plugins.jetbrains.com/)
 */
//todo: rename methods in this interface. "update" -> "plugin"
interface PluginRepository {

  /**
   * Returns all plugins available in the repository.
   */
  fun getAllPlugins(): List<UpdateInfo>

  /**
   * Returns all plugins compatible with this [ideVersion].
   */
  fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<UpdateInfo>

  /**
   * Returns all versions of the plugin with plugin.xml id equal to [pluginId]
   * compatible with [ideVersion].
   */
  fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo>

  /**
   * Returns the last version of the plugin with plugin.xml id equal to [pluginId]
   * compatible with [ideVersion].
   */
  fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? =
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  /**
   * Returns all versions of the plugin with XML ID [pluginId].
   */
  fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo>

  /**
   * Downloads the file of the plugin by its [updateInfo].
   */
  fun downloadPluginFile(updateInfo: UpdateInfo): FileRepositoryResult

  /**
   * Given the [update ID] [updateId], which is a unique identifier
   * of the plugin version in the Plugins Repository database,
   * returns its more detailed [UpdateInfo].
   */
  fun getUpdateInfoById(updateId: Int): UpdateInfo?

  /**
   * Given the [moduleId] returns the ID of the plugin that
   * declares this module.
   */
  fun getIdOfPluginDeclaringModule(moduleId: String): String?

}