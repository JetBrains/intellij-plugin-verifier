package com.jetbrains.pluginverifier.options.filter

import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Implementations of this interface can [dictate][shouldVerifyPlugin]
 * whether a plugin should be verified or ignored.
 */
interface PluginFilter {

  /**
   * Determines whether the [pluginInfo] should
   * be verified or ignored.
   */
  fun shouldVerifyPlugin(pluginInfo: PluginInfo): Result

  sealed class Result {
    object Verify : Result()

    data class Ignore(val reason: String) : Result()
  }

}