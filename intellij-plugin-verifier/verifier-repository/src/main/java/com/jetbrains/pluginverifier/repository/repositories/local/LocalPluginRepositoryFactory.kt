/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.local

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.createIdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.CompatibilityPredicate.Companion.ALWAYS_COMPATIBLE
import com.jetbrains.pluginverifier.repository.repositories.CompatibilityPredicate.Companion.DEFAULT
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepositoryFactory.createLocalPluginRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

/**
 * Utility class that [creates][createLocalPluginRepository] the [LocalPluginRepository].
 */
object LocalPluginRepositoryFactory {

  /**
   * Creates a [LocalPluginRepository] by parsing
   * all [plugin][com.jetbrains.plugin.structure.intellij.plugin.IdePlugin] files under the [repositoryRoot].
   *
   * @param repositoryRoot a root of the local plugin repository that contains plugin artifacts.
   * @param forcePluginCompatibility if `true`, plugins in this repository are compatible with any IDE.
   * No _since build_ or _until build_ checks are made.
   * @param archiveManager manager for ZIP-based plugin artifacts
   * @param problemRemapper plugin problem remapper used for plugin construction.
   */
  fun createLocalPluginRepository(
    repositoryRoot: Path,
    forcePluginCompatibility: Boolean,
    archiveManager: PluginArchiveManager,
    problemRemapper: PluginCreationResultResolver = IntelliJPluginCreationResultResolver()
  ): PluginRepository {
    val pluginFiles = Files.list(repositoryRoot).use { stream ->
      stream
        .filter { it.isDirectory || it.extension == "zip" || it.extension == "jar" }
        .toList()
    }

    val localPluginRepository = LocalPluginRepository(compatibilityPredicate = forcePluginCompatibility.asPredicate())
    for (pluginFile in pluginFiles) {
      createIdePluginManager(archiveManager)
        .createPlugin(pluginFile, validateDescriptor = true, problemResolver = problemRemapper)
        .run {
          if (this is PluginCreationSuccess) {
            localPluginRepository.addLocalPlugin(plugin)
          }
        }
    }
    return localPluginRepository
  }

  private fun Boolean.asPredicate() =
    if (this) ALWAYS_COMPATIBLE else DEFAULT
}