package com.jetbrains.pluginverifier.usages.javaPlugin

interface JavaPluginApiUsageRegistrar {
  fun registerJavaPluginClassUsage(javaPluginClassUsage: JavaPluginClassUsage)
}