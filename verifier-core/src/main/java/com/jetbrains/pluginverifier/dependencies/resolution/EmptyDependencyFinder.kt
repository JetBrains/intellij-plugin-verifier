package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

object EmptyDependencyFinder : DependencyFinder {
  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result =
      DependencyFinder.Result.NotFound("Plugin ${dependency.id} is not found")
}