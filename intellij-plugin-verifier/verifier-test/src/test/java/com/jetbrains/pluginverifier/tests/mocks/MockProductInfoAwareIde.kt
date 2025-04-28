/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.ProductInfoAware
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

class MockProductInfoAwareIde(
  private val idePath: Path,
  override val productInfo: ProductInfo,
  private val bundledPlugins: List<IdePlugin> = emptyList(),
) : Ide(), ProductInfoAware {

  override fun getIdePath() = idePath

  override fun getVersion() = IdeVersion.createIdeVersion(productInfo.version)

  override fun getBundledPlugins() = bundledPlugins
}