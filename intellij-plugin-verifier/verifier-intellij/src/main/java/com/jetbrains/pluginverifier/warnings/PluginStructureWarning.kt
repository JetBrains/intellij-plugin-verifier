/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.warnings

import com.jetbrains.plugin.structure.base.problems.PluginProblem

data class PluginStructureWarning(private val pluginProblem: PluginProblem) {

  init {
    check(pluginProblem.level == PluginProblem.Level.WARNING || pluginProblem.level == PluginProblem.Level.UNACCEPTABLE_WARNING)
  }

  val problemType: String get() = "Plugin descriptor warning"

  val message: String get() = pluginProblem.message
}