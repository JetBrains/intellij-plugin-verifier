package com.jetbrains.plugin.structure.intellij.plugin

data class ModuleV2Dependency(private val id: String) : PluginDependency {
  override fun getId() = id

  override fun isOptional() = true

  override fun isModule() = true

  override fun toString() = "$id (module, v2)"
}