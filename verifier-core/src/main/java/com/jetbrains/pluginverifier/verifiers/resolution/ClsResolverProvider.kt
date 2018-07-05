package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.plugin.PluginDetails

/**
 * Provides a concrete implementation of [ClsResolver]
 * for the current verification.
 */
interface ClsResolverProvider {

  /**
   * Provides a [ClsResolver] to be used in the current verification.
   */
  fun provide(checkedPluginDetails: PluginDetails, resultHolder: ResultHolder): ClsResolver

}