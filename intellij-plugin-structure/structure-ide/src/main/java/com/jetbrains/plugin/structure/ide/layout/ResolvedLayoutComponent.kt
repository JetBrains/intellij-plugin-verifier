/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import java.nio.file.Path

data class ResolvedLayoutComponent(val idePath: Path, val layoutComponent: LayoutComponent) {
    val name: String
      get() = layoutComponent.name

    fun getClasspaths(): List<Path> {
      return if (layoutComponent is LayoutComponent.Classpathable) {
        layoutComponent.getClasspath()
      } else {
        emptyList()
      }
    }

    fun resolveClasspaths(): List<IdeRelativePath> {
      return if (layoutComponent is LayoutComponent.Classpathable) {
        layoutComponent.getClasspath().map { IdeRelativePath(idePath, it) }
      } else {
        emptyList()
      }
    }

    fun allClasspathsExist(): Boolean {
      return resolveClasspaths().all { it.exists }
    }

    val isClasspathable: Boolean
      get() = layoutComponent is LayoutComponent.Classpathable
  }