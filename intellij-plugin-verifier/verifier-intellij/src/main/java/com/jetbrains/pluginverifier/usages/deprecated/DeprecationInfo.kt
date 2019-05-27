package com.jetbrains.pluginverifier.usages.deprecated

/**
 * Contains additional information about deprecated API:
 * - [forRemoval] - whether the deprecated API is scheduled for removal
 * - [untilVersion] - version of the product where API is to be removed
 * (makes sense only if [forRemoval] is true)
 */
data class DeprecationInfo(
    val forRemoval: Boolean,
    val untilVersion: String?
)