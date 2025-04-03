package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId

interface PluginResolverProvider {
  fun getResolver(plugin: IdePlugin): Resolver

  fun contains(pluginId: PluginId): Boolean
}