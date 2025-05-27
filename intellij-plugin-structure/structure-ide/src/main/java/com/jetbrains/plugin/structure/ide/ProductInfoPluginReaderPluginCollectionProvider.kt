/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager.PluginReader
import com.jetbrains.plugin.structure.ide.layout.PluginMetadataSource.ProductInfoSource
import com.jetbrains.plugin.structure.ide.layout.ProductInfoLayoutComponentNameSource
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

class ProductInfoPluginReaderPluginCollectionProvider(private val pluginReader: PluginReader<ProductInfoSource>) : PluginCollectionProvider<Path> {
  override fun getPlugins(source: PluginCollectionSource<Path>): Collection<IdePlugin> {
    if (source !is ProductInfoPluginCollectionSource) {
      return emptySet()
    }
    val (idePath, ideVersion, productInfo) = source
    val layoutComponentNameSource = ProductInfoLayoutComponentNameSource(productInfo)
    val pluginMetadataSource = ProductInfoSource(productInfo)
    return pluginReader.readPlugins(idePath, pluginMetadataSource, layoutComponentNameSource, ideVersion)
  }
}