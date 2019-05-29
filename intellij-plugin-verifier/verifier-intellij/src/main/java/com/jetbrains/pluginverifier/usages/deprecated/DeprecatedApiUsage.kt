package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.usages.ApiUsage

/**
 * Usage of `@Deprecated` or `@deprecated` API.
 */
abstract class DeprecatedApiUsage(val deprecationInfo: DeprecationInfo) : ApiUsage()