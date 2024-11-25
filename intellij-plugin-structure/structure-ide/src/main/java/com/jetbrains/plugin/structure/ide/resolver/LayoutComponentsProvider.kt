/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.ide.layout.LayoutComponents
import com.jetbrains.plugin.structure.ide.layout.MissingClasspathFileInLayoutComponentException
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.*
import com.jetbrains.plugin.structure.ide.layout.ResolvedLayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(LayoutComponentsProvider::class.java)

class LayoutComponentsProvider(private val missingLayoutFileMode: MissingLayoutFileMode) {
  @Throws(MissingClasspathFileInLayoutComponentException::class)
  fun resolveLayoutComponents(productInfo: ProductInfo, idePath: Path): LayoutComponents {
    val layoutComponents = LayoutComponents.of(idePath, productInfo)
    return if (missingLayoutFileMode == IGNORE) {
      layoutComponents
    } else {
      val (okComponents, failedComponents) = layoutComponents.partition { it.allClasspathsExist() }
      if (failedComponents.isNotEmpty()) {
        if (missingLayoutFileMode == FAIL) throw MissingClasspathFileInLayoutComponentException.of(idePath, failedComponents)
        logUnavailableClasspath(failedComponents)
      }
      LayoutComponents(okComponents)
    }
  }

  private fun logUnavailableClasspath(failedComponents: List<ResolvedLayoutComponent>) {
    if (missingLayoutFileMode == SKIP_SILENTLY || !LOG.isWarnEnabled) return
    val logMsg = failedComponents.joinToString("\n") {
      val cp = it.getClasspaths().joinToString(", ")
      "Layout component '${it.name}' has some nonexistent 'classPath' elements: '$cp'"
    }
    LOG.warn(logMsg)
  }
}