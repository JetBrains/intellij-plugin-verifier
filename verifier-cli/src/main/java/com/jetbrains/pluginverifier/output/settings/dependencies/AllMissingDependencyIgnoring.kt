package com.jetbrains.pluginverifier.output.settings.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

object AllMissingDependencyIgnoring : MissingDependencyIgnoring {
  override fun ignoreMissingOptionalDependency(pluginDependency: PluginDependency): Boolean = true
}