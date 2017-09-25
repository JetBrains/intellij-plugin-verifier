package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

sealed class ExtractorResult {
  data class Success(val extractedPlugin: ExtractedPlugin) : ExtractorResult()

  data class Fail(val pluginProblem: PluginProblem) : ExtractorResult()
}