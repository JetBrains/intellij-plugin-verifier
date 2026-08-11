/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.PluginParsingConfigurationResolution
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.empty.EmptyPluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepositoryFactory
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
fun createRepository(repositoryRoot: String?, opts: CmdOpts, archiveManager: PluginArchiveManager): PluginRepository {
  if (repositoryRoot == null) return EmptyPluginRepository
  return LocalPluginRepositoryFactory.createLocalPluginRepository(
    Path.of(repositoryRoot),
    forcePluginCompatibility = opts.offlineMode && opts.forceOfflineCompatibility,
    archiveManager,
    problemRemapper = PluginParsingConfigurationResolution.of(opts)
  )
}

