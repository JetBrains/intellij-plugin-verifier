/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.intellij.platform.ProductInfo

/**
 * Provide layout component names from the `name` key in the `layout` section
 * of the Product Info JSON (`product-info.json`).
 */
class ProductInfoLayoutComponentNameSource(private val productInfo: ProductInfo) :
  LayoutComponentNameSource<PluginMetadataSource.ProductInfoSource> {

  override fun getNames(): List<String> {
    return productInfo.layout.map { it.name }
  }
}