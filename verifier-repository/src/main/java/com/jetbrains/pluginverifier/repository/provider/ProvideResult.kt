package com.jetbrains.pluginverifier.repository.provider

/**
 * Represents possible [provision] [ResourceProvider.provide] outcomes.
 */
sealed class ProvideResult<out R> {
  /**
   * The [resource] is successfully provided.
   */
  data class Provided<out R>(val resource: R) : ProvideResult<R>()

  /**
   * Provision is failed because the specified resource is not found
   * in a place where it used to be.
   * The actual reason can be observed in [reason].
   */
  data class NotFound<R>(val reason: String) : ProvideResult<R>()

  /**
   * The resource was not provided due to network or other failure,
   * reason of which is the [reason] and a thrown exception is [error].
   */
  data class Failed<R>(val reason: String, val error: Exception) : ProvideResult<R>()
}