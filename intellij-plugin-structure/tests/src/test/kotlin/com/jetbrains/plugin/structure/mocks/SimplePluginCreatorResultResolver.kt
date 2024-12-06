/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver

class SimplePluginCreatorResultResolver : PluginCreationResultResolver {
  private val _problems = mutableListOf<PluginProblem>()

  val problems: List<PluginProblem> = _problems

  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationSuccess<IdePlugin> =
    PluginCreationSuccess<IdePlugin>(plugin, problems.remapToWarnings())

  override fun classify(plugin: IdePlugin, problems: List<PluginProblem>): List<ReclassifiedPluginProblem> =
    problems.remapToWarnings()

  private fun List<PluginProblem>.remapToWarnings() = map { problem ->
    ReclassifiedPluginProblem(Level.WARNING, problem)
  }

  fun reset() = _problems.clear()
}