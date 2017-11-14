package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult

open class MockPluginRepositoryAdapter : PluginRepository {
  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> = defaultAction()

  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = defaultAction()

  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> = defaultAction()

  override fun getAllUpdatesOfPlugin(pluginId: String): List<UpdateInfo>? = defaultAction()

  override fun downloadPluginFile(update: UpdateInfo): FileRepositoryResult = defaultAction()

  override fun getUpdateInfoById(updateId: Int): UpdateInfo? = defaultAction()

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = defaultAction()

  override fun getPluginOverviewUrl(pluginInfo: PluginInfo): String? = defaultAction()

  open fun defaultAction(): Nothing = throw AssertionError("No required in tests")


}