 package com.jetbrains.plugin.structure.intellij.plugin

data class PluginV2Dependency(private val id: String) : PluginDependency {
  override fun getId() = id

  override fun isOptional() = false

  override fun isModule() = id.startsWith(INTELLIJ_MODULE_PREFIX)

  override fun toString(): String {
    val moduleFlag = if (isModule) "+module" else ""
    return "$id (plugin$moduleFlag, v2)"
  }
}