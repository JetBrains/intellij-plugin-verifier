package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.usage.ApiUsage

/**
 * Base class for cases of deprecated API usages in bytecode.
 */
abstract class DeprecatedApiUsage(val deprecationInfo: DeprecationInfo) : ApiUsage() {
  companion object {
    private const val serialVersionUID = 0L
  }
}