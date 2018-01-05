package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.extension
import com.jetbrains.pluginverifier.misc.isDirectory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by Sergey.Patrikeev
 */
object LocalPluginRepositoryFactory {
  fun createLocalPluginRepository(ideVersion: IdeVersion, repositoryRoot: Path): LocalPluginRepository {
    return createLocalPluginRepositoryByFiles(ideVersion, repositoryRoot)
  }

  private fun createLocalPluginRepositoryByFiles(ideVersion: IdeVersion, repositoryRoot: Path): LocalPluginRepository {
    val pluginManager = IdePluginManager.createManager()
    val pluginFiles = Files.list(repositoryRoot).filter { it.isDirectory || it.extension == "zip" || it.extension == "jar" }
    val plugins = arrayListOf<LocalPluginInfo>()
    for (pluginFile in pluginFiles) {
      val pluginCreationResult = pluginManager.createPlugin(pluginFile.toFile())
      if (pluginCreationResult is PluginCreationSuccess) {
        val localPluginInfo = createLocalPluginInfo(pluginFile, pluginCreationResult.plugin)
        plugins.add(localPluginInfo)
      }
    }
    return LocalPluginRepository(ideVersion, plugins)
  }

}

fun createLocalPluginInfo(pluginFile: Path, idePlugin: IdePlugin) = LocalPluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginVersion!!,
    pluginFile.toUri().toURL(),
    idePlugin.pluginName ?: "",
    idePlugin.sinceBuild!!,
    idePlugin.untilBuild,
    idePlugin.vendor,
    pluginFile,
    idePlugin.definedModules
)
