/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

internal class PluginLoaderProvider {
  private val loaderRegistry = HashMap<String, PluginLoader<*>>()

  internal fun <C : PluginLoadingContext> register(contextClass: Class<out C>, loader: PluginLoader<C>) {
    loaderRegistry[contextClass.name] = loader
  }
  @Throws(NoSuchElementException::class)
  internal fun <C : PluginLoadingContext> get(contextClass: Class<out C>): PluginLoader<C> {
    @Suppress("UNCHECKED_CAST")
    return loaderRegistry.getValue(contextClass.name) as PluginLoader<C>
  }

  @Throws(NoSuchElementException::class)
  internal inline fun <reified T : PluginLoadingContext, reified L : PluginLoader<T>> get(): L {
    return get(T::class.java) as L
  }
}