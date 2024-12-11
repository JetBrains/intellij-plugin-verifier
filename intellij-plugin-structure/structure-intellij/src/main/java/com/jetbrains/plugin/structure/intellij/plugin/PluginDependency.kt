package com.jetbrains.plugin.structure.intellij.plugin

interface PluginDependency {
  val id: String
  val isOptional: Boolean
  val isModule: Boolean

  fun asOptional(): PluginDependency
}