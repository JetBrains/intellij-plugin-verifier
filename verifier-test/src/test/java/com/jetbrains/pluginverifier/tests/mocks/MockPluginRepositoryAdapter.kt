package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import java.net.URL

open class MockPluginRepositoryAdapter : PluginRepository {
  override fun getAllPlugins(): List<UpdateInfo> = defaultAction()

  override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> = defaultAction()

  override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = defaultAction()

  override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> = defaultAction()

  override fun getAllUpdatesOfPlugin(pluginId: String): List<UpdateInfo> = defaultAction()

  override fun downloadPluginFile(updateInfo: UpdateInfo): FileRepositoryResult = defaultAction()

  override fun getUpdateInfoById(updateId: Int): UpdateInfo? = defaultAction()

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = defaultAction()

  open fun defaultAction(): Nothing = throw AssertionError("Not required in tests")

}

private val exampleUrl = URL("http://example.com")

fun createMockUpdateInfo(pluginId: String, pluginName: String, version: String, updateId: Int) =
    UpdateInfo(
        pluginId,
        version,
        pluginName,
        updateId,
        "",
        "",
        "",
        exampleUrl,
        exampleUrl,
        exampleUrl
    )