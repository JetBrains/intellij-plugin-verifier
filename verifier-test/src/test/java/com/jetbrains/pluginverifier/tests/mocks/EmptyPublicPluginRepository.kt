package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult

/**
 * Created by Sergey.Patrikeev
 */
object EmptyPublicPluginRepository : PluginRepository {
  override fun getAllPlugins(): List<UpdateInfo> = emptyList()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<UpdateInfo> = emptyList()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = null

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> = emptyList()

  override fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo> = emptyList()

  override fun downloadPluginFile(updateInfo: UpdateInfo): FileRepositoryResult = FileRepositoryResult.NotFound("")

  override fun getUpdateInfoById(updateId: Int): UpdateInfo? = null

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = null

}