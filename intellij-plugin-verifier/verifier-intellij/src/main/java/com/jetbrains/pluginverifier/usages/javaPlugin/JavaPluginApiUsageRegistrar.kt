package com.jetbrains.pluginverifier.usages.javaPlugin

interface JavaPluginApiUsageRegistrar {
  fun registerJavaPluginClassUsage(classUsage: JavaPluginClassUsage)
}