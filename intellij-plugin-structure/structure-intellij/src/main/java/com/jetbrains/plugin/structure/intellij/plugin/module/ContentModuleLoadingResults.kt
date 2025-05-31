/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar

class ContentModuleLoadingResults : ProblemRegistrar {
  private val _contentModules = mutableListOf<ResolvedContentModuleWithDescriptor>()
  val contentModules: List<ResolvedContentModuleWithDescriptor>
    get() = _contentModules

  private val _problems = mutableListOf<PluginProblem>()
  val problems: List<PluginProblem>
    get() = _problems

  fun add(resolvedContentModule: IdePlugin, moduleDescriptor: ModuleDescriptor) {
    _contentModules += ResolvedContentModuleWithDescriptor(resolvedContentModule, moduleDescriptor)
  }

  override fun registerProblem(problem: PluginProblem) {
    _problems += problem
  }

  data class ResolvedContentModuleWithDescriptor(val contentModule: IdePlugin, val descriptor: ModuleDescriptor)
}