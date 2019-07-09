package com.jetbrains.pluginverifier.resolution

import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver

/**
 * Provides a concrete implementation of [ClassResolver]
 * for the current verification.
 */
interface ClassResolverProvider {

  /**
   * Provides a [ClassResolver] to be used in the current verification.
   */
  fun provide(checkedPluginDetails: PluginDetails, verificationResult: VerificationResult): ClassResolver

}