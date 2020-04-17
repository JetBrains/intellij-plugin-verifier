/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.plugin

import java.util.*

abstract class PluginProblem {

  abstract val level: Level

  abstract val message: String

  enum class Level {
    ERROR,
    WARNING
  }

  final override fun toString() = message

  final override fun equals(other: Any?) = other is PluginProblem
    && level == other.level && message == other.message

  final override fun hashCode() = Objects.hash(message, level)

}