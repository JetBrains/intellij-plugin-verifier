package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location

data class JavaPluginClassUsage(val usedClass: ClassLocation, val usageLocation: Location)