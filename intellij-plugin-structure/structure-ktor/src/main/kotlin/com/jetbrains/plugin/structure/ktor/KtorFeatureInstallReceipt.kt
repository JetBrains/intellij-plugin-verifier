/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.ktor.bean.POSITION_FILE
import com.jetbrains.plugin.structure.ktor.bean.POSITION_INSIDE
import com.jetbrains.plugin.structure.ktor.bean.POSITION_OUTSIDE
import com.jetbrains.plugin.structure.ktor.bean.POSITION_TESTFUN

data class KtorFeatureInstallReceipt(
  val imports: List<String> = emptyList(),
  val installBlock: String? = null,
  val extraTemplates: List<CodeTemplate> = emptyList(),
)

data class CodeTemplate(
  val position: Position,
  val text: String
)

enum class Position {
  INSIDE_APPLICATION_MODULE,
  OUTSIDE_APPLICATION_MODULE,
  SEPARATE_FILE,
  TEST_FUNCTION
}
