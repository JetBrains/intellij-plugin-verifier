/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule

object NegativeIdeModulePredicate : IdeModulePredicate {
  override fun matches(id: String, plugin: IdePlugin) = false
}

class DefaultIdeModulePredicate(private val moduleIdentifiers: Set<PluginId>) : IdeModulePredicate {
  override fun matches(id: String, plugin: IdePlugin): Boolean {
    return plugin is IdeModule || id in moduleIdentifiers
  }
}

class ProductInfoBasedIdeModulePredicate(private val productInfo: ProductInfo) : IdeModulePredicate {
  override fun matches(id: String, plugin: IdePlugin): Boolean {
    return plugin is IdeModule || id in productInfo.modules
  }
}

/**
 * Predicate for a plugin that is considered to be a module if it identifier or alias starts with [idPrefix].
 */
class IdPrefixIdeModulePredicate(private val idPrefix: String) : IdeModulePredicate {
  override fun matches(id: String, plugin: IdePlugin): Boolean {
    return plugin is IdeModule
      || id.startsWith(idPrefix)
      || plugin.pluginAliases.any { it.startsWith(idPrefix) }
  }

  companion object {
    val HAS_COM_INTELLIJ_MODULE_PREFIX = IdPrefixIdeModulePredicate("com.intellij.modules.")
  }
}