/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult.Type.MODULE
import com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult.Type.PLUGIN
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(CompositePluginProvider::class.java)

class CompositePluginProvider(private val pluginProviders: Collection<PluginProvider>) : PluginProvider {
  override fun findPluginById(pluginId: String): IdePlugin? = firstNotNullOfOrNull(pluginId) {
    it.findPluginById(pluginId)
  }

  override fun findPluginByModule(moduleId: String): IdePlugin? = firstNotNullOfOrNull(moduleId) {
    it.findPluginByModule(moduleId)
  }

  private inline fun firstNotNullOfOrNull(id: String, transform: (PluginProvider) -> IdePlugin?): IdePlugin? {
    for (provider in pluginProviders) {
      LOG.debug("Searching for plugin or module '{}' in '{}'", id, provider.presentableName)
      transform(provider)?.let { return it.also {
        LOG.debug("Found plugin or module '{}' in '{}'", id, provider.presentableName)
      }}
    }
    return null
  }

  override fun findPluginByIdOrModuleId(pluginIdOrModuleId: String): PluginProviderResult? {
    for (provider in pluginProviders) {
      provider
        .findPluginById(pluginIdOrModuleId)
        ?.let { PluginProviderResult(PLUGIN, it) }
        ?: provider.findPluginByModule(pluginIdOrModuleId)
          ?.let { PluginProviderResult(MODULE, it) }
    }
    return null
  }

  override fun query(query: PluginQuery): PluginProvision {
    for (provider in pluginProviders) {
      val provision = provider.query(query)
      if (provision is PluginProvision.Found) {
        return provision
      }
    }
    return PluginProvision.NotFound
  }

  companion object {
    fun of(vararg pluginProviders: PluginProvider) = CompositePluginProvider(pluginProviders.toList())
  }
}