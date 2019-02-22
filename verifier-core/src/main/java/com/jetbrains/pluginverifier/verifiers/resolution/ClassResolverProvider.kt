package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.reporting.verification.Reporters

/**
 * Provides a concrete implementation of [ClassResolver]
 * for the current verification.
 */
interface ClassResolverProvider {

  /**
   * Provides a [ClassResolver] to be used in the current verification.
   */
  fun provide(checkedPluginDetails: PluginDetails, resultHolder: ResultHolder, pluginReporters: Reporters): ClassResolver

}