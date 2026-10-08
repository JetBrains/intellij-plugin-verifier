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
data class NodeId(val pluginId: PluginId, val moduleId: PluginId?) {
  companion object {
    fun ofPlugin(plugin: IdePlugin) : NodeId {
      val id = plugin.pluginId
      requireNotNull(id) { missingId(plugin) }
      return NodeId(id, null)
    }
  }
}

interface PluginAware {
  val plugin: IdePlugin
}

sealed class Dependency {
  abstract fun matches(id: PluginId): Boolean

  abstract val isTransitive: Boolean

  abstract val nodeId: NodeId?

  sealed class Resolved : Dependency(), PluginAware

  /**
   * A `<content><module name="..."/></content>` declaration in the owning [plugin]'s main descriptor.
   * Represents the owne rship dependency from the main plugin module to the content module [id],
   * rather than a resolved module reference declared in `<dependencies>`.
   */
  data class ContentModuleDeclaration(override val plugin: IdePlugin, val id: PluginId) : Resolved() {
    override fun matches(id: PluginId) = this.id == id

    override val isTransitive = false

    // Cached on first access: graph construction reads `nodeId` very frequently. Not part of equals/hashCode.
    @Volatile
    private var cachedNodeId: NodeId? = null

    override val nodeId: NodeId
      get() = cachedNodeId ?: NodeId(plugin.pluginId!!, id).also { cachedNodeId = it }

    override fun toString() = "Content module '$id' declared by plugin '${plugin.pluginId}'"
  }

  data class Module(override val plugin: IdePlugin, val id: PluginId, override val isTransitive: Boolean = false) : Resolved() {
    override fun matches(id: PluginId) = plugin.pluginId == id || plugin.hasDefinedModuleWithId(id)

    // Cached on first access: graph construction reads `nodeId` very frequently. Not part of equals/hashCode.
    @Volatile
    private var cachedNodeId: NodeId? = null

    override val nodeId: NodeId
      get() = cachedNodeId ?: NodeId(plugin.pluginId!!, id).also { cachedNodeId = it }

    override fun toString() =
      "${if (isTransitive) "Transitive " else ""}Module '$id' provided by plugin '${plugin.pluginId}'"
  }

  data class Plugin(override val plugin: IdePlugin, override val isTransitive: Boolean = false) : Resolved() {
    override fun matches(id: PluginId) = plugin.pluginId == id

    // Cached on first access: graph construction reads `nodeId` very frequently. Not part of equals/hashCode.
    @Volatile
    private var cachedNodeId: NodeId? = null

    override val nodeId: NodeId
      get() = cachedNodeId ?: NodeId(plugin.pluginId!!, null).also { cachedNodeId = it }

    override fun toString() = "${if (isTransitive) "Transitive " else ""}Plugin dependency: '${plugin.pluginId}'"
  }

  object None : Dependency() {
    override fun matches(id: PluginId) = false
    override val isTransitive = false
    override val nodeId: NodeId? = null
  }
}
