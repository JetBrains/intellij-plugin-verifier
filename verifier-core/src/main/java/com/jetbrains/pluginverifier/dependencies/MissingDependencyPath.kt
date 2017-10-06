package com.jetbrains.pluginverifier.dependencies

data class MissingDependencyPath(val path: List<DependencyNode>,
                                 val missingDependency: MissingDependency) {
  override fun toString(): String = path.joinToString(" ---X--> ") + " ---X--> " + missingDependency
}