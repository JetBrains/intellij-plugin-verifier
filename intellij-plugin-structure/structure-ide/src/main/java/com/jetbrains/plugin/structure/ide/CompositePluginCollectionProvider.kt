/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

class CompositePluginCollectionProvider<S> private constructor(private val providers: List<PluginCollectionProvider<S>>) :
  PluginCollectionProvider<S> {
  override fun getPlugins(source: PluginCollectionSource<S, *>): List<IdePlugin> = providers.flatMap { it.getPlugins(source) }

  companion object {
    fun <S> of(providers: List<PluginCollectionProvider<S>>) =
      CompositePluginCollectionProvider(providers)

    fun <S> of(vararg providers: PluginCollectionProvider<S>) =
      CompositePluginCollectionProvider(providers.toList())
  }
}