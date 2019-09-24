package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginClassFileOrigin
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class JavaPluginApiUsageProcessor(private val javaPluginApiUsageRegistrar: JavaPluginApiUsageRegistrar) : ApiUsageProcessor {
  override fun processClassReference(classReference: ClassReference, resolvedClass: ClassFile, usageLocation: Location, context: VerificationContext) {
    if (resolvedClass.isJavaPluginApi()) {
      javaPluginApiUsageRegistrar.registerJavaPluginClassUsage(
          JavaPluginClassUsage(resolvedClass.containingClassFile.location, usageLocation)
      )
    }
  }

  private fun ClassFileMember.isJavaPluginApi(): Boolean {
    val pluginClassFileOrigin = containingClassFile.classFileOrigin.findOriginOfType<PluginClassFileOrigin>()
    return pluginClassFileOrigin != null && pluginClassFileOrigin.idePlugin.pluginId == "com.intellij.java"
  }

}