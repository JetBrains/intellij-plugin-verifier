/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.intellij.platform.ProductInfo

sealed class PluginMetadataSource {
  data class ProductInfoSource(val productInfo: ProductInfo) : PluginMetadataSource()
  data class LayoutComponentsSource(val layoutComponents: LayoutComponents) : PluginMetadataSource()
  object None : PluginMetadataSource()
}