/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.intellij.plugin.ModuleVisibility

/**
 * Reported when a content module attempts to depend on another content module
 * that is not visible according to the module visibility rules.
 */
data class ModuleVisibilityProblem(
  val dependingModuleName: String,
  val dependingPluginId: String,
  val targetModuleName: String,
  val targetPluginId: String,
  val targetVisibility: ModuleVisibility,
  val dependingNamespace: String?,
  val targetNamespace: String?
) : CompatibilityProblem() {

  override val problemType: String
    get() = "Module visibility violation"

  override val shortDescription: String
    get() = when (targetVisibility) {
      ModuleVisibility.PRIVATE ->
        "Module '$dependingModuleName' illegally accesses private module '$targetModuleName'"
      ModuleVisibility.INTERNAL ->
        "Module '$dependingModuleName' illegally accesses internal module '$targetModuleName'"
      ModuleVisibility.PUBLIC ->
        "Unexpected visibility violation for public module '$targetModuleName'"
    }

  override val fullDescription: String
    get() = when (targetVisibility) {
      ModuleVisibility.PRIVATE ->
        "Module '$dependingModuleName' (plugin '$dependingPluginId') cannot depend on " +
          "private module '$targetModuleName' from plugin '$targetPluginId'. " +
          "Private modules are only accessible within the same plugin. " +
          "This can lead to **module loading failure** at runtime."

      ModuleVisibility.INTERNAL ->
        "Module '$dependingModuleName' (namespace: '${dependingNamespace ?: "none"}') cannot depend on " +
          "internal module '$targetModuleName' (namespace: '${targetNamespace ?: "none"}'). " +
          "Internal modules are only accessible from modules with the same namespace. " +
          "This can lead to **module loading failure** at runtime."

      ModuleVisibility.PUBLIC ->
        "Unexpected visibility violation for public module '$targetModuleName'."
    }
}