package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginClassFileOrigin
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class JavaPluginApiUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is JavaPluginApiUsageRegistrar && classFileMember.isJavaPluginApi()) {
      context.registerJavaPluginClassUsage(
          JavaPluginClassUsage(classFileMember.containingClassFile.location, usageLocation)
      )
    }
  }

  private fun ClassFileMember.isJavaPluginApi(): Boolean {
    val pluginClassFileOrigin = containingClassFile.classFileOrigin.findOriginOfType<PluginClassFileOrigin>()
    return pluginClassFileOrigin != null && pluginClassFileOrigin.idePlugin.pluginId == "com.intellij.java"
  }

}