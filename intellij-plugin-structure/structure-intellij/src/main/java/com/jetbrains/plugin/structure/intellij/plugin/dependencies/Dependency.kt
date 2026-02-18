/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

/**
 * Identifies a dependency graph node as a (plugin, module) tuple.
 *
 * For plugin dependencies, [moduleId] is `null` and only [pluginId] is set.
 * For module dependencies, [moduleId] is non-null and [pluginId] refers to the plugin that provides the module.
 */
data class NodeId(val pluginId: PluginId, val moduleId: PluginId?)

interface PluginAware {
  val plugin: IdePlugin
}

sealed class Dependency {
  abstract fun matches(id: PluginId): Boolean

  abstract val isTransitive: Boolean

  abstract val nodeId: NodeId?

  data class Module(override val plugin: IdePlugin, val id: PluginId, override val isTransitive: Boolean = false) : Dependency(), PluginAware {
    override fun matches(id: PluginId) = plugin.pluginId == id || plugin.hasDefinedModuleWithId(id)

    override val nodeId: NodeId get() = NodeId(plugin.pluginId!!, id)

    override fun toString() =
      "${if (isTransitive) "Transitive " else ""}Module '$id' provided by plugin '${plugin.pluginId}'"
  }

  data class Plugin(override val plugin: IdePlugin, override val isTransitive: Boolean = false) : Dependency(), PluginAware {
    override fun matches(id: PluginId) = plugin.pluginId == id

    override val nodeId: NodeId get() = NodeId(plugin.pluginId!!, null)

    override fun toString() = "${if (isTransitive) "Transitive " else ""}Plugin dependency: '${plugin.pluginId}'"
  }

  object None : Dependency() {
    override fun matches(id: PluginId) = false
    override val isTransitive = false
    override val nodeId: NodeId? = null
  }
}
