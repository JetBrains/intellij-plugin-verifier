package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult

interface PluginRepository {

  fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo>

  fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo?

  fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo>

  fun getAllUpdatesOfPlugin(pluginId: String): List<UpdateInfo>?

  fun downloadPluginFile(update: UpdateInfo): FileRepositoryResult

  fun getUpdateInfoById(updateId: Int): UpdateInfo?

  fun getIdOfPluginDeclaringModule(moduleId: String): String?

  fun getPluginOverviewUrl(pluginInfo: PluginInfo): String?

}