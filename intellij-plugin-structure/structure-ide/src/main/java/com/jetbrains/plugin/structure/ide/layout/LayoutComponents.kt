/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.ide.problem.IdeProblem
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import java.nio.file.Path

class LayoutComponents(
  val layoutComponents: List<ResolvedLayoutComponent>,
  val problems: List<IdeProblem> = emptyList()
) :
  Iterable<ResolvedLayoutComponent> {
  companion object {
    fun of(idePath: Path, productInfo: ProductInfo): LayoutComponents {
      val resolvedLayoutComponents = productInfo.layout
        .map { ResolvedLayoutComponent(idePath, it) }
      return LayoutComponents(resolvedLayoutComponents)
    }
  }

  override fun iterator() = layoutComponents.iterator()

  val content: List<LayoutComponent>
    get() = layoutComponents.map { it.layoutComponent }
}