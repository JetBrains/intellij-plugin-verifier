/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.options.repository

import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.PluginParsingConfigurationResolution
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepositoryFactory
import java.nio.file.Path

object LocalPluginRepositoryProvider {

  fun getLocalPluginRepository(opts: CmdOpts, downloadDirectory: Path): Result {
    return if (!opts.offlineMode) {
      Result.Unavailable
    } else {
      LocalPluginRepositoryFactory.createLocalPluginRepository(
        downloadDirectory,
        opts.forceOfflineCompatibility,
        PluginParsingConfigurationResolution.of(opts)
      ).let { Result.Provided(it) }
    }
  }

  sealed class Result {
    data class Provided(val pluginRepository: PluginRepository) : Result()
    object Unavailable : Result()
  }
}