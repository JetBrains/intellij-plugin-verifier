package com.jetbrains.plugin.structure.intellij.plugin

data class ModuleDescriptor(
  val name: String,
  val dependencies: List<PluginDependency>,
  val module: IdePlugin,
  val configurationFilePath: String
)