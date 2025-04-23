/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import java.io.Closeable

/**
 * Searches for the plugin dependencies in places specified by the implementations and creates corresponding plugin details.
 */
interface DependencyFinder {

  /**
   * Presentable name of this finder, like "bundled plugins of IDE IU-192.345".
   */
  val presentableName: String

  /**
   * Finds a plugin or a module that corresponds to the [dependency].
   * The possible results are represented as instances of the [Result].
   */
  fun findPluginDependency(dependencyId: String, isModule: Boolean): Result

  /**
   * Finds a plugin or a module that corresponds to the [dependency].
   * The possible results are represented as instances of the [Result].
   */
  fun findPluginDependency(dependency: PluginDependency): Result

  /**
   * Represents possible results of the [findPluginDependency].
   * It must be closed after usage to release the [DetailsProvided.pluginDetailsCacheResult].
   */
  sealed class Result : Closeable {

    /**
     * The [dependency] [PluginDependency] is resolved and
     * the corresponding [details] [com.jetbrains.pluginverifier.plugin.PluginDetails]
     * were fetched from the [PluginDetailsCache].
     *
     * The resolution result is represented by [PluginDetailsCache.Result]
     * to indicate that the dependent plugin could be invalid.
     */
    data class DetailsProvided(val pluginDetailsCacheResult: PluginDetailsCache.Result) : Result() {
      override fun close() = pluginDetailsCacheResult.close()
    }

    /**
     * The dependency is resolved to the [plugin].
     */
    data class FoundPlugin(val plugin: IdePlugin, val origin: DependencyOrigin = DependencyOrigin.Unknown) : Result() {
      override fun close() = Unit
    }

    /**
     * The dependency is not resolved because of the [reason].
     */
    data class NotFound(val reason: String) : Result() {
      override fun close() = Unit
    }
  }
}

sealed class DependencyOrigin {
  object Bundled : DependencyOrigin()
  object Unknown : DependencyOrigin()
}