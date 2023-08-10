package com.jetbrains.pluginverifier.output

enum class OutputFormat {
  STREAM,
  HTML,
  MARKDOWN;

  fun code() = this.name.lowercase()
}

val DEFAULT_OUTPUT_FORMATS = listOf(OutputFormat.STREAM, OutputFormat.HTML)