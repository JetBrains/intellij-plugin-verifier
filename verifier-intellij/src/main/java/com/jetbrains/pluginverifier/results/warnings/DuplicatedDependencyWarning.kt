package com.jetbrains.pluginverifier.results.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependencyNode

data class DuplicatedDependencyWarning(
    val verifiedPlugin: DependencyNode,
    val dependencyId: String
) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message: String
    get() = "Plugin $verifiedPlugin has duplicated dependency declaration: $dependencyId"
}