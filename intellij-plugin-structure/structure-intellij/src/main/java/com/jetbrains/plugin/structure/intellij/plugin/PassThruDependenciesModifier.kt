/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */


package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Keeps the plugin's exposed dependencies, including resolved content-module dependencies in standard plugin implementations.
 * Preserves reasons and contribution sources from preceding modifiers instead of reconstructing the original declarations.
 * Main-descriptor-only collection requires an explicit [DependenciesModifier] using [IdePlugin.reconstructDependencies].
 */
object PassThruDependenciesModifier : DependenciesModifier {
  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification> =
    plugin.getDependencyModifications()
}