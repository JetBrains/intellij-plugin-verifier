/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.javaPlugin

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin.BundledPlugin
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.util.isFromVerifiedPlugin
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

  private fun ClassFileMember.isJavaPluginApi(): Boolean {
    with(containingClassFile.classFileOrigin) {
      val bundledPluginOrigin = findOriginOfType<BundledPlugin>()
      if (bundledPluginOrigin != null) {
        return bundledPluginOrigin.isJavaPlugin()
      }
      val pluginOrigin = findOriginOfType<PluginFileOrigin>()
      if (pluginOrigin != null) {
        return pluginOrigin.isJavaPlugin()
      }
      return false
    }
  }

  private fun BundledPlugin.isJavaPlugin() = idePlugin.isJavaPlugin()
  private fun PluginFileOrigin.isJavaPlugin() = idePlugin.isJavaPlugin()
  private fun IdePlugin.isJavaPlugin() = pluginId == "com.intellij.java"
}