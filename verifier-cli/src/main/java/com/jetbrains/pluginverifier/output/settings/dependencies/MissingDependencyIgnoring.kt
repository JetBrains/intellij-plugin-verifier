package com.jetbrains.pluginverifier.output.settings.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

/**
 * @author Sergey Patrikeev
 */
interface MissingDependencyIgnoring {
  fun ignoreMissingOptionalDependency(pluginDependency: PluginDependency): Boolean
}