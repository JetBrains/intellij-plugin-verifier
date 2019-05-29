package com.jetbrains.pluginverifier.usages.deprecated

interface DeprecatedApiRegistrar {
  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage)
}