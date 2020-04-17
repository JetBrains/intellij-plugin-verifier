/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

object ForbiddenWordInPluginName : PluginProblem() {
  val forbiddenWords = listOf("teamcity", "plugin")

  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin name should not contain the following words: ${forbiddenWords.joinToString()}"

}