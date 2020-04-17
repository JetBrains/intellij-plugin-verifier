/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType

class JavaPluginApiUsageProcessor(private val javaPluginApiUsageRegistrar: JavaPluginApiUsageRegistrar) : ApiUsageProcessor {
  override fun processClassReference(classReference: ClassReference, resolvedClass: ClassFile, context: VerificationContext, referrer: ClassFileMember, classUsageType: ClassUsageType) {
    if (resolvedClass.isJavaPluginApi() && context.isFromVerifiedPlugin(referrer)) {
      javaPluginApiUsageRegistrar.registerJavaPluginClassUsage(
        JavaPluginClassUsage(resolvedClass.containingClassFile.location, referrer.location)
      )
    }
  }

  private fun VerificationContext.isFromVerifiedPlugin(fileMember: ClassFileMember): Boolean {
    val pluginFileOrigin = fileMember.containingClassFile.classFileOrigin.findOriginOfType<PluginFileOrigin>()
    return this is PluginVerificationContext && idePlugin == pluginFileOrigin?.idePlugin
  }

  private fun ClassFileMember.isJavaPluginApi(): Boolean {
    val pluginFileOrigin = containingClassFile.classFileOrigin.findOriginOfType<PluginFileOrigin>()
    return pluginFileOrigin != null && pluginFileOrigin.idePlugin.pluginId == "com.intellij.java"
  }

}