package com.jetbrains.pluginverifier.utils

object CompactJsonUtil {

  val DELIMITER: String = "#"

  private val UNESCAPED_DELIMITER_REGEX: Regex = Regex("(?<!\\\\)\\$DELIMITER")

  private fun escape(string: String): String = string.replace(DELIMITER, "\\$DELIMITER")

  private fun unescape(string: String): String = string.replace("\\$DELIMITER", DELIMITER)

  fun serialize(parts: List<String>): String = parts.joinToString(separator = DELIMITER, transform = { escape(it) })

  fun deserialize(string: String): List<String> = string.split(UNESCAPED_DELIMITER_REGEX).map { unescape(it) }

}