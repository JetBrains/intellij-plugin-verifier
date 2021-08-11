/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.SymbolicReference

/**
 * Base class for all usages of API in bytecode.
 */
abstract class ApiUsage {

  /**
   * API reference in bytecode.
   */
  abstract val apiReference: SymbolicReference

  /**
   * API element to which the [apiReference] resolves.
   */
  abstract val apiElement: Location

  /**
   * Exact location in bytecode where [apiElement] is used
   */
  abstract val usageLocation: Location

  /**
   * Short description of this API usage
   */
  abstract val shortDescription: String

  /**
   * Full description of this API usage
   */
  abstract val fullDescription: String

  /**
   * Description of the API usage type
   */
  abstract val problemType: String

  /**
   * `equals` must be implemented in inheritors
   */
  abstract override fun equals(other: Any?): Boolean

  /**
   * `hashCode` must be implemented in inheritors
   */
  abstract override fun hashCode(): Int

  final override fun toString() = fullDescription

}