package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.ClassLocation

class SameOriginApiUsageFilter : ClassLocationApiUsageFilter() {
  override fun allow(usageLocation: ClassLocation, apiLocation: ClassLocation): Boolean {
    val usageOrigin = usageLocation.classFileOrigin
    val apiHostOrigin = apiLocation.classFileOrigin

    return usageOrigin == apiHostOrigin
  }
}