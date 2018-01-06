package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.misc.extension
import com.jetbrains.pluginverifier.misc.isDirectory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by Sergey.Patrikeev
 */
object LocalPluginRepositoryFactory {
  fun createLocalPluginRepository(repositoryRoot: Path): LocalPluginRepository {
    return createLocalPluginRepositoryByFiles(repositoryRoot)
  }

  private fun createLocalPluginRepositoryByFiles(repositoryRoot: Path): LocalPluginRepository {
    val pluginManager = IdePluginManager.createManager()
    val pluginFiles = Files.list(repositoryRoot).filter { it.isDirectory || it.extension == "zip" || it.extension == "jar" }
    val localPluginRepository = LocalPluginRepository()
    val plugins = arrayListOf<LocalPluginInfo>()
    for (pluginFile in pluginFiles) {
      val pluginCreationResult = pluginManager.createPlugin(pluginFile.toFile())
      if (pluginCreationResult is PluginCreationSuccess) {
        val localPluginInfo = createLocalPluginInfo(pluginFile, pluginCreationResult.plugin, localPluginRepository)
        plugins.add(localPluginInfo)
      }
    }
    localPluginRepository.plugins.addAll(plugins)
    return localPluginRepository
  }

}

fun createLocalPluginInfo(pluginFile: Path, idePlugin: IdePlugin, pluginRepository: LocalPluginRepository) = LocalPluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginVersion!!,
    pluginFile.toUri().toURL(),
    pluginRepository,
    idePlugin.pluginName ?: "",
    idePlugin.sinceBuild!!,
    idePlugin.untilBuild,
    idePlugin.vendor,
    pluginFile, idePlugin.definedModules
)
