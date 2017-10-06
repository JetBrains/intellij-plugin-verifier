package com.jetbrains.pluginverifier.dependencies

data class DependencyNode(val id: String,
                          val version: String,
                          val missingDependencies: List<MissingDependency>) {
  override fun toString(): String = "$id:$version"
}