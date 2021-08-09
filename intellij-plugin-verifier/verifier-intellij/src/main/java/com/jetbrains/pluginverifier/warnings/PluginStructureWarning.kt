/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

data class PluginStructureWarning(private val pluginProblem: PluginProblem) {

  init {
    check(pluginProblem.level == PluginProblem.Level.WARNING)
  }

  val problemType: String get() = "Plugin descriptor warning"

  val description: String get() = "Represents a plugin's structure waring"

  val message: String get() = pluginProblem.message
}