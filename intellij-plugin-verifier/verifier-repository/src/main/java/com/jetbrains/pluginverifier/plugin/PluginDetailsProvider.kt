/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider.Result
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable

/**
 * [Provides] [providePluginDetails] the [PluginDetails] of the plugins.
 * Possible results are represented as instances of [Result].
 */
interface PluginDetailsProvider {

  /**
   * Creates [PluginDetails] for existing plugin.
   */
  fun providePluginDetails(pluginInfo: PluginInfo, idePlugin: IdePlugin): Result

  /**
   * Creates the [PluginDetails] for [plugin] [pluginInfo] whose
   * file is locked with [pluginFileLock].
   */
  fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock): Result

  /**
   * Creates [PluginDetails] for existing plugin with warnings of the plugin structure.
   */
  fun providePluginDetails(pluginInfo: PluginInfo, idePlugin: IdePlugin, warnings: List<PluginProblem>): Result

  /**
   * Represents possible results of [providing] [providePluginDetails] the [PluginDetails].
   */
  sealed class Result : Closeable {

    data class Provided(val pluginDetails: PluginDetails) : Result() {
      override fun close() = pluginDetails.close()
    }

    data class InvalidPlugin(val pluginInfo: PluginInfo, val pluginErrors: List<PluginProblem>) : Result() {
      override fun close() = Unit
    }

    data class Failed(val reason: String, val error: Exception) : Result() {
      override fun close() = Unit
    }
  }

}