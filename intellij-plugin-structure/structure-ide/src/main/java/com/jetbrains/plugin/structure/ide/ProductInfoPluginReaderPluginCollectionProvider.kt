/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginReader
import com.jetbrains.plugin.structure.ide.layout.ProductInfoLayoutComponentNameSource
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

/**
 * Loads bundled [IdePlugin]s by delegating to a [PluginReader] that works directly with
 * [ProductInfo] (the raw `product-info.json` data).
 *
 * This is the simpler, non-layout-aware counterpart to
 * [ProductInfoLayoutBasedPluginCollectionProvider]. Instead of decomposing the IDE layout into
 * core / layout / additional phases, it hands the entire [ProductInfo] to the supplied
 * [pluginReader] and returns whatever that reader produces.
 *
 * This provider only handles [ProductInfoPluginCollectionSource] sources; any other source type
 * results in an empty collection.
 *
 * @param pluginReader reader that turns [ProductInfo] into a collection of [IdePlugin]s.
 */
class ProductInfoPluginReaderPluginCollectionProvider(private val pluginReader: PluginReader<ProductInfo>) : PluginCollectionProvider<Path> {
  override fun getPlugins(source: PluginCollectionSource<Path, *>): Collection<IdePlugin> {
    if (source !is ProductInfoPluginCollectionSource) {
      return emptySet()
    }
    val (idePath, ideVersion, productInfo) = source
    val layoutComponentNameSource = ProductInfoLayoutComponentNameSource(productInfo)
    return pluginReader.readPlugins(idePath, productInfo, layoutComponentNameSource, ideVersion)
  }
}