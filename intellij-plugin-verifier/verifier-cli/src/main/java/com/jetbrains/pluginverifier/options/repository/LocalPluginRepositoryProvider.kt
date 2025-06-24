/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.options.repository

import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.remapping.JsonUrlProblemLevelRemappingManager
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginParsingConfiguration
import com.jetbrains.pluginverifier.options.PluginParsingConfigurationResolution
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepositoryFactory
import java.nio.file.Path

object LocalPluginRepositoryProvider {

  private val pluginParsingConfigurationResolution = PluginParsingConfigurationResolution()

  fun getLocalPluginRepository(opts: CmdOpts, downloadDirectory: Path): Result {
    return if (!opts.offlineMode) {
      Result.Unavailable
    } else {
      LocalPluginRepositoryFactory.createLocalPluginRepository(
        downloadDirectory,
        opts.forceOfflineCompatibility,
        opts.problemResolver
      ).let { Result.Provided(it) }
    }
  }

  // TODO duplicate with PluginsParsing
  private val PluginParsingConfiguration.problemResolver: PluginCreationResultResolver
    get() = pluginParsingConfigurationResolution.resolveProblemLevelMapping(
      this,
      JsonUrlProblemLevelRemappingManager.fromClassPathJson()
    )

  private val CmdOpts.problemResolver: PluginCreationResultResolver
    get() = OptionsParser.createPluginParsingConfiguration(this).problemResolver

  sealed class Result {
    data class Provided(val pluginRepository: PluginRepository) : Result()
    object Unavailable : Result()
  }
}