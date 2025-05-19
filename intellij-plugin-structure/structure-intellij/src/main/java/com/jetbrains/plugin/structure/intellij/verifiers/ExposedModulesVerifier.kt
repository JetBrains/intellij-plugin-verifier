/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.ProhibitedModuleExposed

private val jetBrainsModuleCommonPrefixes = setOf("com.intellij", "org.jetbrains", "intellij")

class ExposedModulesVerifier {
  fun verify(plugin: IdePlugin, problemRegistrar: ProblemRegistrar, descriptorPath: String? = null) {
    val prohibitedModules = plugin.definedModules.mapNotNull { moduleName ->
      jetBrainsModuleCommonPrefixes
        .firstOrNull { moduleName.startsWith(it) }
        ?.let { ProhibitedModuleName(moduleName, it) }
    }

    if (prohibitedModules.isNotEmpty()) {
      problemRegistrar.registerProblem(ProhibitedModuleExposed(descriptorPath, prohibitedModules))
    }
  }

  data class ProhibitedModuleName(val moduleName: String, val prohibitedPrefix: String)
}