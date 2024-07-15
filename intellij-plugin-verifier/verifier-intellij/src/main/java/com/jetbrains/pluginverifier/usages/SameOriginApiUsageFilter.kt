package com.jetbrains.pluginverifier.usages

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation

object SameOriginApiUsageFilter : ClassLocationApiUsageFilter() {
  override fun allow(usageLocation: ClassLocation, apiLocation: ClassLocation): Boolean {
    val usageOrigin = usageLocation.classFileOrigin
    val apiHostOrigin = apiLocation.classFileOrigin
    return invoke(usageOrigin, apiHostOrigin)
  }

  operator fun invoke(usageOrigin: FileOrigin, apiHostOrigin: FileOrigin): Boolean = usageOrigin == apiHostOrigin
}
