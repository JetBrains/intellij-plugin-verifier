package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.DownloadPluginResult
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * Created by Sergey.Patrikeev
 */
object EmptyPublicPluginRepository : PluginRepository {
  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> = emptyList()

  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = null

  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> = emptyList()

  override fun getAllUpdatesOfPlugin(pluginId: String): List<UpdateInfo>? = null

  override fun downloadPluginFile(update: UpdateInfo): DownloadPluginResult = DownloadPluginResult.NotFound("")

  override fun getUpdateInfoById(updateId: Int): UpdateInfo? = null

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = null

  override fun getPluginOverviewUrl(update: UpdateInfo): String? = null
}