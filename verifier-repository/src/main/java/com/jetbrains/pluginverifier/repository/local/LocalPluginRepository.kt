package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import java.net.URL
import java.nio.file.Path

class LocalPluginRepository(override val repositoryURL: URL,
                            val plugins: MutableList<LocalPluginInfo> = arrayListOf()) : PluginRepository {
  companion object {
    private val VERSION_COMPARATOR = compareBy<LocalPluginInfo, String>(VersionComparatorUtil.COMPARATOR, { it.version })
  }

  //todo: log the creation failure. It is the verification result.
  fun addPlugin(pluginFile: Path): LocalPluginInfo? {
    val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile.toFile())
    return when (pluginCreationResult) {
      is PluginCreationFail<IdePlugin> -> null
      is PluginCreationSuccess<IdePlugin> -> {
        val plugin = pluginCreationResult.plugin
        LocalPluginInfo(
            plugin.pluginId!!,
            plugin.pluginVersion!!,
            this,
            plugin.pluginName ?: "",
            plugin.sinceBuild!!,
            plugin.untilBuild,
            plugin.vendor,
            pluginFile,
            plugin.definedModules
        )
      }
    }
  }

  override fun getAllPlugins() = plugins

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
      plugins.filter { it.isCompatibleWith(ideVersion) }
          .groupBy { it.pluginId }
          .mapValues { it.value.maxWith(VERSION_COMPARATOR)!! }
          .values.toList()

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      plugins.filter { it.isCompatibleWith(ideVersion) && it.pluginId == pluginId }

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId).maxWith(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
      plugins.filter { it.pluginId == pluginId }

  override fun getPluginInfoById(updateId: Int) = null

  override fun getIdOfPluginDeclaringModule(moduleId: String) =
      plugins.find { moduleId in it.definedModules }?.pluginId

  override fun downloadPluginFile(pluginInfo: PluginInfo) =
      FileRepositoryResult.Found(IdleFileLock((pluginInfo as LocalPluginInfo).pluginFile))

  fun findPluginById(pluginId: String): LocalPluginInfo? = plugins.find { it.pluginId == pluginId }

  fun findPluginByModule(moduleId: String): LocalPluginInfo? = plugins.find { moduleId in it.definedModules }
}