/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.problems.PluginProblem

sealed class ExtractorResult {
  data class Success(val extractedPlugin: ExtractedPlugin) : ExtractorResult()

  data class Fail(val pluginProblem: PluginProblem) : ExtractorResult()
}