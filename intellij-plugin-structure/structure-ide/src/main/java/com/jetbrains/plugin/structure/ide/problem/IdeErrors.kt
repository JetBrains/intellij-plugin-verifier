/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.problem

import com.jetbrains.plugin.structure.ide.layout.IdeRelativePath

class LayoutComponentHasNonExistentClasspath(
  val layoutComponentName: String,
  val offendingClasspathElements: List<IdeRelativePath> = emptyList()
) : IdeProblem() {
  override val level = Level.ERROR
  override val message: String
    get() {
      val cp = offendingClasspathElements.map { it.relativePath }.joinToString(", ")
      return "Layout component '${layoutComponentName}' has some nonexistent 'classPath' elements: '$cp'"
    }
}