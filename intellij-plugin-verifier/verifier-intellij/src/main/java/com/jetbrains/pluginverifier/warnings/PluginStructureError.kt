/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

/**
 * Represents a fatal plugin's structure error,
 * such as missing mandatory field in the plugin descriptor (`<id>`, `<version>`, etc.).
 */
data class PluginStructureError(private val pluginProblem: PluginProblem) {
  init {
    check(pluginProblem.level == PluginProblem.Level.ERROR)
  }

  val problemType: String
    get() = "Plugin descriptor error"

  val message: String
    get() = pluginProblem.message

  val description = "Represents a fatal plugin's structure error, such as missing mandatory field in the plugin descriptor (`<id>`, `<version>`, etc.)"

  override fun toString() = message
}