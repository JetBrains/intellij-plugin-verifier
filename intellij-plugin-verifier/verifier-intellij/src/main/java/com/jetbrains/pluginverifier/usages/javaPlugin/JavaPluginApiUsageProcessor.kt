package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginClassFileOrigin
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class JavaPluginApiUsageProcessor(private val javaPluginApiUsageRegistrar: JavaPluginApiUsageRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(apiReference: SymbolicReference, resolvedMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (resolvedMember.isJavaPluginApi()) {
      javaPluginApiUsageRegistrar.registerJavaPluginClassUsage(
          JavaPluginClassUsage(resolvedMember.containingClassFile.location, usageLocation)
      )
    }
  }

  private fun ClassFileMember.isJavaPluginApi(): Boolean {
    val pluginClassFileOrigin = containingClassFile.classFileOrigin.findOriginOfType<PluginClassFileOrigin>()
    return pluginClassFileOrigin != null && pluginClassFileOrigin.idePlugin.pluginId == "com.intellij.java"
  }

}