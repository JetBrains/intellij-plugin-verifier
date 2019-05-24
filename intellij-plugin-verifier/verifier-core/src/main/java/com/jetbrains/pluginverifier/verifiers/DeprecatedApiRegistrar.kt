package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage

interface DeprecatedApiRegistrar {
  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage)

  object Empty : DeprecatedApiRegistrar {
    override fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) = Unit
  }
}