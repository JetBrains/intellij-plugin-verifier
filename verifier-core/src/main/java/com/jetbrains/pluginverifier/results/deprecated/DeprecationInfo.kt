package com.jetbrains.pluginverifier.results.deprecated

import java.io.Serializable

/**
 * Contains additional information about deprecated API:
 * - [forRemoval] - whether the deprecated API is scheduled for removal
 * - [untilVersion] - version of the product where API is to be removed
 * (makes sense only if [forRemoval] is true)
 */
data class DeprecationInfo(
    val forRemoval: Boolean,
    val untilVersion: String?
) : Serializable {

  companion object {
    private const val serialVersionUID = 0L
  }

}