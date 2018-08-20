package com.jetbrains.pluginverifier.results.usage

import com.jetbrains.pluginverifier.results.location.Location
import java.io.Serializable

/**
 * Base class for all usages of API in bytecode.
 */
abstract class ApiUsage : Serializable {

  /**
   * API element being used
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
   * `equals` must be implemented in inheritors
   */
  abstract override fun equals(other: Any?): Boolean

  /**
   * `hashCode` must be implemented in inheritors
   */
  abstract override fun hashCode(): Int

  final override fun toString() = fullDescription

  companion object {
    private const val serialVersionUID = 0L
  }

}