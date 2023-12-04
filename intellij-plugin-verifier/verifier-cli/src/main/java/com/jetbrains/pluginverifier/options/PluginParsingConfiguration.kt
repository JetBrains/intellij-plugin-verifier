package com.jetbrains.pluginverifier.options

import com.jetbrains.pluginverifier.options.SubmissionType.NEW

enum class SubmissionType {
  NEW,
  EXISTING
}

data class PluginParsingConfiguration(val pluginSubmissionType: SubmissionType = NEW, val readPluginLogos: Boolean = true)
