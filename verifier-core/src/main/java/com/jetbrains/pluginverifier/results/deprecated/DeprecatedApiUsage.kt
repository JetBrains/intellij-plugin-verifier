package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.Location
import java.io.Serializable

/**
 * Base class for cases of deprecated API usages in bytecode.
 */
abstract class DeprecatedApiUsage : Serializable {
  abstract val deprecationInfo: DeprecationInfo

  abstract val deprecatedElement: Location

  abstract val usageLocation: Location

  abstract val shortDescription: String

  abstract val fullDescription: String

  abstract val deprecatedElementType: DeprecatedElementType

  abstract override fun equals(other: Any?): Boolean

  abstract override fun hashCode(): Int

  final override fun toString() = fullDescription
}