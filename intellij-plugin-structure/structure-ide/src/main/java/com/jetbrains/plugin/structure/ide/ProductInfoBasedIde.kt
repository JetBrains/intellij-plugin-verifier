/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

class ProductInfoBasedIde(
  private val idePath: Path,
  private val version: IdeVersion,
  private val bundledPlugins: List<IdePlugin>,
  val productInfo: ProductInfo
) : Ide() {
  override fun getIdePath() = idePath

  override fun getVersion() = version

  override fun getBundledPlugins() = bundledPlugins
}