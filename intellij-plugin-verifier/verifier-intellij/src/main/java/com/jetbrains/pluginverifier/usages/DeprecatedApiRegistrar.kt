package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage

interface DeprecatedApiRegistrar {
  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage)
}