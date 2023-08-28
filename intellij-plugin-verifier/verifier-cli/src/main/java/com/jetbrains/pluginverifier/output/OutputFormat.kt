package com.jetbrains.pluginverifier.output

enum class OutputFormat {
  PLAIN,
  HTML,
  MARKDOWN;

  fun code() = this.name.lowercase()
}

val DEFAULT_OUTPUT_FORMATS = listOf(OutputFormat.PLAIN, OutputFormat.HTML)