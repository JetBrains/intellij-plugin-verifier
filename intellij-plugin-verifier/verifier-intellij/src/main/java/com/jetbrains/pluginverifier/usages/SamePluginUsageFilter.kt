/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.pluginverifier.usages

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation

/**
 * API Usage filter that allows class usages, method invocations and field references
 * within the same JAR, directory or similar common origins.
 *
 * This filter will ignore API usages that occur within the same plugin.
 */
class SamePluginUsageFilter : ClassLocationApiUsageFilter() {
  override fun allow(usageLocation: ClassLocation, apiLocation: ClassLocation): Boolean {
    val usageOrigin = usageLocation.classFileOrigin
    val apiHostOrigin = apiLocation.classFileOrigin
    return isInvocationWithinSameOrigin(usageOrigin, apiHostOrigin)
      || isInvocationWithinPlatform(usageOrigin, apiHostOrigin)
      || isInvocationWithinSamePlugin(usageOrigin, apiHostOrigin)
  }

  private fun isInvocationWithinSamePlugin(usageOrigin: FileOrigin, apiHostOrigin: FileOrigin) =
    usageOrigin == apiHostOrigin.findOriginOfType<PluginFileOrigin>()

  private fun isInvocationWithinPlatform(usageOrigin: FileOrigin, apiHostOrigin: FileOrigin) =
    apiHostOrigin.isOriginOfType<IdeFileOrigin>()
      && (usageOrigin == apiHostOrigin || usageOrigin.isOriginOfType<IdeFileOrigin>())

  private fun isInvocationWithinSameOrigin(usageOrigin: FileOrigin, apiHostOrigin: FileOrigin): Boolean {
    return SameOriginApiUsageFilter(usageOrigin, apiHostOrigin)
  }
}