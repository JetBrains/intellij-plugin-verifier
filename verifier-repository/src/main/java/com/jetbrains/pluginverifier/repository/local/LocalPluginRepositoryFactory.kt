package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.pluginverifier.misc.extension
import com.jetbrains.pluginverifier.misc.isDirectory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by Sergey.Patrikeev
 */
object LocalPluginRepositoryFactory {
  fun createLocalPluginRepository(repositoryRoot: Path): LocalPluginRepository {
    val pluginFiles = Files.list(repositoryRoot).filter {
      it.isDirectory || it.extension == "zip" || it.extension == "jar"
    }

    val localPluginRepository = LocalPluginRepository(repositoryRoot.toUri().toURL())
    for (pluginFile in pluginFiles) {
      localPluginRepository.addLocalPlugin(pluginFile)
    }
    return localPluginRepository
  }

}