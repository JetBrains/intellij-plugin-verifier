package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.extension
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.repository.local.meta.LocalRepositoryMetadataParser
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
        val localPluginInfo = with(pluginCreationResult.plugin) {
          LocalPluginInfo(pluginId!!, pluginVersion!!, pluginName ?: pluginId!!, sinceBuild!!, untilBuild, vendor, pluginFile, definedModules)
        }
        plugins.add(localPluginInfo)
      }
    }
    return LocalPluginRepository(ideVersion, plugins)
  }

  /**
   * The plugins.xml meta-file doesn't have the definition of modules contained in the plugins
   * so we actually have to read the plugin descriptors.
   */
  private fun createLocalPluginRepositoryByMetadata(repositoryMetadataXml: Path, ideVersion: IdeVersion): LocalPluginRepository {
    val plugins = try {
      LocalRepositoryMetadataParser().parseFromXml(repositoryMetadataXml)
    } catch (e: Exception) {
      throw IllegalArgumentException("Unable to parse meta-file $repositoryMetadataXml", e)
    }
    return LocalPluginRepository(ideVersion, plugins)
  }

}