package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

fun interface DependenciesProvider {
  fun getDependencies(plugin: IdePlugin): Set<Dependency>
}