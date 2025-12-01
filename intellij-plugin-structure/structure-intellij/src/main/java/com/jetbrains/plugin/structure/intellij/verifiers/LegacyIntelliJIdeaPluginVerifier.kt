/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.plugin.structure.intellij.problems.NoDependencies
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier.VerificationResult.NotLegacyPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(LegacyIntelliJIdeaPluginVerifier::class.java)

private const val PLATFORM_MODULE_ID = "com.intellij.modules.platform"
private val ADDITIONAL_MODULES_AVAILABLE_IN_ALL_PRODUCTS = listOf(
  "com.intellij.modules.platform",
  "com.intellij.modules.lang",
  "com.intellij.modules.xml",
  "com.intellij.modules.vcs",
  "com.intellij.modules.xdebugger"
)

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

    val dependencies = plugin.dependencies
    if (dependencies.isEmpty()) {
      return VerificationResult.NoDependencies
    } else {
      val v1Dependencies = dependencies.filterIsInstance<PluginV1Dependency>()
      if (dependsOnAnyModuleAvailableInAllProducts(v1Dependencies)) return NotLegacyPlugin
      if (dependsOnAnyModuleWithComIntellijModulesPrefix(v1Dependencies)) return NotLegacyPlugin
      if (dependencies.any { it is PluginV2Dependency }) return NotLegacyPlugin

      return VerificationResult.NoModuleDependencies
    }
  }

  private fun dependsOnAnyModuleWithComIntellijModulesPrefix(dependencies: List<PluginV1Dependency>): Boolean {
    return dependencies.any { it.id.startsWith(INTELLIJ_MODULE_PREFIX) }
  }

  private fun dependsOnAnyModuleAvailableInAllProducts(dependencies: List<PluginDependency>): Boolean {
    if (dependencies.any { it.id == PLATFORM_MODULE_ID }) {
      return true
    } else {
      LOG.debug("Undeclared dependency on module '$PLATFORM_MODULE_ID'. " +
                  "Plugin should declare this dependency to indicate dependence on shared functionality")
    }
    if (dependencies.any { it.id in ADDITIONAL_MODULES_AVAILABLE_IN_ALL_PRODUCTS }) {
      return true
    } else {
      LOG.debug("Undeclared dependency on any of the modules that are available in all Products." +
                  "This is not an issue if a dependency on the '$PLATFORM_MODULE_ID$ is declared explicitly.")
    }
    return false
  }
}