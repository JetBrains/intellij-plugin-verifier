/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a dependency on a content module (`<dependencies><module name=""/>` statement).
 * If `namespace` attribute isn't specified, [namespace] of the referenced module is computed automatically (the
 * namespace of the parent plugin is used if the module from the same plugin is referenced, otherwise the default
 * `jetbrains` namespace is used).
 */
class ContentModuleDependency(
  val moduleName: String,
  val namespace: String,
) {
  override fun toString(): String = "ModuleDependency(name='$moduleName')"
}
