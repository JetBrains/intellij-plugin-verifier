package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import java.net.URL

open class MockPluginRepositoryAdapter : PluginRepository {
  override fun getAllPlugins(): List<PluginInfo> = defaultAction()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = defaultAction()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = defaultAction()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> = defaultAction()

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> = defaultAction()

  override fun downloadPluginFile(pluginInfo: PluginInfo): FileRepositoryResult = defaultAction()

  override fun getPluginInfoById(updateId: Int): UpdateInfo? = defaultAction()

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = defaultAction()

  open fun defaultAction(): Nothing = throw AssertionError("Not required in tests")

  fun createMockUpdateInfo(pluginId: String, pluginName: String, version: String, updateId: Int) =
      UpdateInfo(
          pluginId,
          version,
          exampleUrl,
          this@MockPluginRepositoryAdapter,
          pluginName,
          updateId,
          "",
          "",
          "",
          exampleUrl, exampleUrl
      )

}

private val exampleUrl = URL("http://example.com")

