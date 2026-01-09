/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.intellij.problems.UndeclaredKotlinK2CompatibilityMode
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationDescriptor.IDE
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning

/**
 Remaps [UndeclaredKotlinK2CompatibilityMode] structure [com.jetbrains.plugin.structure.base.problems.PluginProblem]
 to a Plugin Verifier [CompatibilityProblem] if the IDE version is at least 2024.2.1.

 The `structure` library reports the [UndeclaredKotlinK2CompatibilityMode] as a warning.
 However, depending on the Platform version, this warning might be either ignored or resolved to a Plugin Verifier
 [CompatibilityProblem].

 This resolver modifies the [PluginVerificationContext] by removing all instances of [UndeclaredKotlinK2CompatibilityMode]s
 from the [PluginVerificationContext.pluginStructureWarnings].
 */
class KotlinCompatibilityModeProblemResolver : CompatibilityProblemResolver {
  companion object {
    private val sinceIdeVersion = IdeVersion.createIdeVersion("242.21829.142")
  }

  /**
   * Remaps any [UndeclaredKotlinK2CompatibilityMode] structure warning to an [UndeclaredKotlinK2CompatibilityModeWarning]
   * and removes it from the [plugin verification context][PluginVerificationContext].
   */
  override fun resolveCompatibilityWarnings(context: PluginVerificationContext): List<CompatibilityWarning> {
    if (!context.isSinceSupportedIdeVersion()) {
      context.removeUndeclaredKotlinK2CompatibilityModeProblems()
      return emptyList()
    }

    return context.pluginStructureWarnings
      .map { it.problem }
      .filterIsInstance<UndeclaredKotlinK2CompatibilityMode>()
      .map { UndeclaredKotlinK2CompatibilityModeWarning(it) }
      .apply {
        if (isNotEmpty()) {
          context.removeUndeclaredKotlinK2CompatibilityModeProblems()
        }
      }
  }

  private fun PluginVerificationContext.removeUndeclaredKotlinK2CompatibilityModeProblems() {
    pluginStructureWarnings.removeIf { it.problem is UndeclaredKotlinK2CompatibilityMode }
  }

  override fun resolveCompatibilityProblems(context: PluginVerificationContext) = emptyList<CompatibilityProblem>()

  private fun PluginVerificationContext.isSinceSupportedIdeVersion(): Boolean {
    if (verificationDescriptor !is IDE) return false
    return verificationDescriptor.ideVersion >= sinceIdeVersion
  }
}