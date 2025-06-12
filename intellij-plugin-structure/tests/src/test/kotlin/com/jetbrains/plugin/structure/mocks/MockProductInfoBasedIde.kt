/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.ProductInfoAware
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

/**
 * Mock IDE for `product-info.json` based layouts.
 *
 * For legacy style mock IDEs, see [MockIde].
 *
 */
data class MockProductInfoBasedIde(
  private val idePath: Path,
  override val productInfo: ProductInfo,
  private val bundledPlugins: List<IdePlugin> = emptyList(),
) : Ide(), ProductInfoAware {

  private val ideVersion = IdeVersion.createIdeVersion(productInfo.productCode + "-" + productInfo.buildNumber)

  override fun getIdePath() = idePath

  override fun getVersion() = ideVersion

  override fun getBundledPlugins() = bundledPlugins
}