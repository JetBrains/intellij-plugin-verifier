/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.local

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepositoryFactory.createLocalPluginRepository
import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility class that [creates] [createLocalPluginRepository] the [LocalPluginRepository].
 */
object LocalPluginRepositoryFactory {

  /**
   * Creates a [LocalPluginRepository] by parsing
   * all [plugin] [com.jetbrains.plugin.structure.intellij.plugin.IdePlugin] files under the [repositoryRoot].
   */
  fun createLocalPluginRepository(repositoryRoot: Path): PluginRepository {
    val pluginFiles = Files.list(repositoryRoot).filter {
      it.isDirectory || it.extension == "zip" || it.extension == "jar"
    }

    val localPluginRepository = LocalPluginRepository()
    for (pluginFile in pluginFiles) {
      with(IdePluginManager.createManager().createPlugin(pluginFile.toFile())) {
        when (this) {
          is PluginCreationSuccess -> localPluginRepository.addLocalPlugin(plugin)
          is PluginCreationFail -> Unit
        }
      }
    }
    return localPluginRepository
  }

}