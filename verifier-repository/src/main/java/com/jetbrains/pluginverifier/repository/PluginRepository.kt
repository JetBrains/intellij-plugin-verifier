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
  fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo>

  /**
   * Returns all versions of the plugin with plugin.xml id equal to [pluginId]
   * compatible with [ideVersion].
   */
  fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo>

  /**
   * Returns the last version of the plugin with plugin.xml id equal to [pluginId]
   * compatible with [ideVersion].
   */
  fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? =
      getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId).maxBy { it.updateId }

  /**
   * Returns all versions of the plugin with XML ID [pluginId].
   */
  fun getAllUpdatesOfPlugin(pluginId: String): List<UpdateInfo>

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