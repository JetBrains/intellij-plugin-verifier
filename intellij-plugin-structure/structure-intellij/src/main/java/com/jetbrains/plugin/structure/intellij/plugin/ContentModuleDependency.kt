/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a dependency on a content module (`<dependencies><module name=""/>` statement)
 */
class ContentModuleDependency(
  /** It's called name in the xml, but we want to refer to it as id now */
  val moduleId: String
) {
  override fun toString(): String = "ModuleDependency(moduleId='$moduleId')"
}
