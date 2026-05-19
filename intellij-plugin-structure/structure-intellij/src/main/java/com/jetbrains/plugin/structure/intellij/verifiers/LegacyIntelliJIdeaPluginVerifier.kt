/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.plugin.structure.intellij.problems.NoDependencies
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier.VerificationResult.NotLegacyPlugin

/**
 * Verifies if a plugin is a legacy plugin compatible with IntelliJ IDEA only.
 *
 * See [IntelliJ SDK Plugin Compatibility Docs](https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html#declaring-plugin-dependencies)
 * for detailed info.
 */
class LegacyIntelliJIdeaPluginVerifier {
  sealed class VerificationResult {
    object NotLegacyPlugin : VerificationResult()
    object NoDependencies: VerificationResult()
    object NoModuleDependencies: VerificationResult()
  }

  fun verify(plugin: IdePlugin, descriptorPath: String, problemRegistrar: ProblemRegistrar) {
    when (verify(plugin)) {
      NotLegacyPlugin -> Unit
      VerificationResult.NoDependencies -> problemRegistrar.registerProblem(NoDependencies(descriptorPath))
      VerificationResult.NoModuleDependencies -> problemRegistrar.registerProblem(NoModuleDependencies(descriptorPath))
    }
  }

  fun verify(plugin: IdePlugin): VerificationResult {
    if (plugin is IdeModule || plugin.hasPackagePrefix || plugin.contentModules.isNotEmpty()) return NotLegacyPlugin
    if (plugin.hasAnyV2Dependencies()) return NotLegacyPlugin

    val dependencies = plugin.dependencies
    if (dependencies.isEmpty()) {
      return VerificationResult.NoDependencies
    } else {
      val v1Dependencies = dependencies.filterIsInstance<PluginV1Dependency>()
      // Due to confusing semantics we might need to check old-style module declarations
      val oldSemanticsModuleDependencies = dependencies.filterIsInstance<PluginDependencyImpl>()
      val moduleCandidates = v1Dependencies + oldSemanticsModuleDependencies
      if (moduleCandidates.any { it.id.startsWith(INTELLIJ_MODULE_PREFIX) }) return NotLegacyPlugin

      return VerificationResult.NoModuleDependencies
    }
  }

  private fun IdePlugin.hasAnyV2Dependencies() = dependencies.any { it is PluginV2Dependency || it is ModuleV2Dependency }
    || contentModuleDependencies.isNotEmpty()
    || pluginMainModuleDependencies.isNotEmpty()
}