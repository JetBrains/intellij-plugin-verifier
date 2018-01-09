package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder.Result.DetailsProvided
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import java.io.Closeable

/**
 * [Finds] [findPluginDependency] the [plugin dependencies] [PluginDependency]
 * in places specified by the implementations and creates
 * the corresponding [plugin details] [com.jetbrains.pluginverifier.plugin.PluginDetails].
 */
interface DependencyFinder {

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
    data class FoundPlugin(val plugin: IdePlugin) : Result() {
      override fun close() = Unit
    }

    /**
     * The dependency is not resolved because of the [reason].
     */
    data class NotFound(val reason: String) : Result() {
      override fun close() = Unit
    }

    /**
     * The dependency points to a default module of the verified IDE.
     */
    data class DefaultIdeModule(val moduleId: String) : Result() {
      override fun close() = Unit
    }
  }
}