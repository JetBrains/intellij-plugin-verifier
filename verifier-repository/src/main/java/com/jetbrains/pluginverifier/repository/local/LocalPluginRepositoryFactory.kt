package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.local.meta.LocalRepositoryMetadataParser
import java.io.File

/**
 * Created by Sergey.Patrikeev
 */
object LocalPluginRepositoryFactory {
  fun createLocalPluginRepository(ideVersion: IdeVersion, repositoryRoot: File): LocalPluginRepository {
    val repositoryMetadataXml = repositoryRoot.resolve("plugins.xml")
    return if (repositoryMetadataXml.exists()) {
      createLocalPluginRepositoryByMetadata(repositoryMetadataXml, ideVersion)
    } else {
      createLocalPluginRepositoryByFiles(ideVersion, repositoryRoot)
    }
  }

  private fun createLocalPluginRepositoryByFiles(ideVersion: IdeVersion, repositoryRoot: File): LocalPluginRepository {
    val pluginManager = IdePluginManager.createManager()
    val pluginFiles = repositoryRoot.listFiles().filter { it.isDirectory || it.extension == "zip" || it.extension == "jar" }
    val plugins = arrayListOf<LocalPluginInfo>()
    for (pluginFile in pluginFiles) {
      val pluginCreationResult = pluginManager.createPlugin(pluginFile)
      if (pluginCreationResult is PluginCreationSuccess) {
        val localPluginInfo = with(pluginCreationResult.plugin) {
          LocalPluginInfo(pluginId!!, pluginVersion!!, pluginName ?: pluginId!!, sinceBuild!!, untilBuild, vendor, pluginFile)
        }
        plugins.add(localPluginInfo)
      }
    }
    return LocalPluginRepository(ideVersion, plugins)
  }

  private fun createLocalPluginRepositoryByMetadata(repositoryMetadataXml: File, ideVersion: IdeVersion): LocalPluginRepository {
    val plugins = try {
      LocalRepositoryMetadataParser().parseFromXml(repositoryMetadataXml)
    } catch (e: Exception) {
      throw IllegalArgumentException("Unable to parse meta-file $repositoryMetadataXml", e)
    }
    return LocalPluginRepository(ideVersion, plugins)
  }

}