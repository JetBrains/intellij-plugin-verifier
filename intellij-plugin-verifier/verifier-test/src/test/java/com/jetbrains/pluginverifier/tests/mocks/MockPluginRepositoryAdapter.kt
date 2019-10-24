package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository

open class MockPluginRepositoryAdapter : PluginRepository {

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = defaultAction()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo? = defaultAction()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> = defaultAction()

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> = defaultAction()

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = defaultAction()

  open fun defaultAction(): Nothing = throw AssertionError("Not required in tests")

}

fun createMockPluginInfo(pluginId: String, version: String): PluginInfo =
  object : PluginInfo(pluginId, pluginId, version, null, null, null) {}
