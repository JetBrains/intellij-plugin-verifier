/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.provider

/**
 * Represents possible [provision] [ResourceProvider.provide] outcomes.
 */
sealed class ProvideResult<out R : Any> {
  /**
   * The [resource] is successfully provided.
   */
  data class Provided<out R : Any>(val resource: R) : ProvideResult<R>()

  /**
   * Provision is failed because the specified resource is not found
   * in a place where it used to be.
   * The actual reason can be observed in [reason].
   */
  data class NotFound<out R : Any>(val reason: String) : ProvideResult<R>()

  /**
   * The resource was not provided due to network or other failure,
   * reason of which is the [reason] and a thrown exception is [error].
   */
  data class Failed<out R : Any>(val reason: String, val error: Exception) : ProvideResult<R>()
}