package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.usages.ApiUsage

/**
 * Base class for cases of deprecated API usages in bytecode.
 */
abstract class DeprecatedApiUsage(val deprecationInfo: DeprecationInfo) : ApiUsage()