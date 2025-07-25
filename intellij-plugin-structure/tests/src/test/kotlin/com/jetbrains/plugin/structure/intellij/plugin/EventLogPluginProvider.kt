/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.ide.PluginQueryMatcher

class EventLogSinglePluginProvider(private val plugin: IdePlugin) : PluginProvider {
  private val pluginQueryMatcher = PluginQueryMatcher()

  private val _pluginSearchLog = mutableListOf<LogEntry>()
  val pluginSearchLog: List<LogEntry> = _pluginSearchLog

  override fun findPluginById(pluginId: String): IdePlugin? {
    return if (plugin.pluginId == pluginId) {
      plugin.also {
        _pluginSearchLog += LogEntry(pluginId, plugin, "plugin")
      }
    } else {
      null
    }
  }

  override fun findPluginByModule(moduleId: String): IdePlugin? {
    return if (moduleId in plugin.definedModules) {
      plugin.also {
        _pluginSearchLog += LogEntry(moduleId, plugin, "module")
      }
    } else {
      null
    }
  }

  override fun query(query: PluginQuery): PluginProvision {
    return pluginQueryMatcher.matches(plugin, query).also {
      _pluginSearchLog += LogEntry(query.identifier, plugin, it.toReason())
    }
  }

  fun clear() {
    _pluginSearchLog.clear()
  }

  private fun PluginProvision.toReason(): String {
    return when (this) {
      is PluginProvision.Found -> "found via $source"
      PluginProvision.NotFound -> "not found"
    }
  }

  data class LogEntry(val pluginId: String, val plugin: IdePlugin?, val reason: String)
}