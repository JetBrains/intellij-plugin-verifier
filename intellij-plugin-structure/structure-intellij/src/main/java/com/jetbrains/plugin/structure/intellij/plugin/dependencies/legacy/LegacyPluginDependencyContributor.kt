/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy

import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier.VerificationResult.NotLegacyPlugin

private const val CORE_IDE_PLUGIN_ID = "com.intellij"

private const val ALL_MODULES_ID = "com.intellij.modules.all"
private const val JAVA_MODULE_ID = "com.intellij.modules.java"
private const val UNKNOWN_PLUGIN_ID = "Unknown Plugin"

private val JAVA_MODULE_DEPENDENCY = PluginDependencyImpl(JAVA_MODULE_ID, false, true)

/**
 * Contributes a _Java Module_ as a dependency to legacy plugin.
 *
 * If a plugin doesn't declare any dependencies in its `plugin.xml` file, or if it declares dependencies only on
  * other plugins but not modules, it is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA.
 */
class LegacyPluginDependencyContributor(private val ide: PluginProvider, private val legacyPluginVerifier: LegacyIntelliJIdeaPluginVerifier): DependenciesModifier {
  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification> {
    val dependencies = plugin.getDependencyModifications()

    if (plugin.pluginId == CORE_IDE_PLUGIN_ID) {
      return dependencies
    }
    if (ide.findPluginByModule(ALL_MODULES_ID) == null) {
      return dependencies
    }
    val isNonBundledPlugin = plugin.isNonBundled(ide)
    if (isNonBundledPlugin && plugin.isLegacy()) {
      val javaModule = ide.findPluginByModule(JAVA_MODULE_ID)
      if (javaModule != null) {
        return dependencies + (JAVA_MODULE_DEPENDENCY to DependencyModificationReason.IDE)
      }
    }
    return dependencies
  }

  private fun IdePlugin.isLegacy() =
    legacyPluginVerifier.verify(this) != NotLegacyPlugin

  private fun IdePlugin.isNonBundled(pluginProvider: PluginProvider): Boolean {
    return pluginId?.let { id ->
      !pluginProvider.containsPlugin(id)
    } ?: false
  }
}