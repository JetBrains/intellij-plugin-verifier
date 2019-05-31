package com.jetbrains.pluginverifier.usages.nonExtendable

interface NonExtendableApiRegistrar {
  fun registerNonExtendableApiUsage(nonExtendableApiUsage: NonExtendableApiUsage)
}