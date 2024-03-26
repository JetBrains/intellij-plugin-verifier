package com.jetbrains.pluginverifier.options

import com.jetbrains.pluginverifier.options.SubmissionType.NEW

enum class SubmissionType {
  NEW,
  EXISTING
}

typealias ProblemId = String

data class PluginParsingConfiguration(val pluginSubmissionType: SubmissionType = NEW,
                                      val ignoredPluginProblems: List<ProblemId> = emptyList())
