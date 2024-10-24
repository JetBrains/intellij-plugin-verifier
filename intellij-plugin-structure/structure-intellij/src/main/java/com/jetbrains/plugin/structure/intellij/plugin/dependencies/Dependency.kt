package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

interface PluginAware {
  val plugin: IdePlugin
}

sealed class Dependency {
  abstract fun matches(id: PluginId): Boolean

  abstract val isTransitive: Boolean

  data class Module(override val plugin: IdePlugin, val id: PluginId, override val isTransitive: Boolean = false) : Dependency(), PluginAware {
    override fun matches(id: PluginId) = plugin.pluginId == id

    override fun toString() =
      "${if (isTransitive) "Transitive " else ""}Module '$id' provided by plugin '${plugin.pluginId}'"
  }

  data class Plugin(override val plugin: IdePlugin, override val isTransitive: Boolean = false) : Dependency(), PluginAware {
    override fun matches(id: PluginId) = plugin.pluginId == id

    override fun toString() = "${if (isTransitive) "Transitive " else ""}Plugin dependency: '${plugin.pluginId}'"
  }

  object None : Dependency() {
    override fun matches(id: PluginId) = false
    override val isTransitive = false
  }
}
