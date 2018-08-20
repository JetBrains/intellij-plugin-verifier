package com.jetbrains.pluginverifier.results.experimental

import com.jetbrains.pluginverifier.results.usage.ApiUsage

/**
 * Describes case of ApiStatus.Experimental API usage in bytecode.
 */
abstract class ExperimentalApiUsage : ApiUsage() {

  companion object {
    private const val serialVersionUID = 0L
  }

}