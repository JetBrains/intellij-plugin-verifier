package com.jetbrains.pluginverifier.misc

import org.atteo.evo.inflector.English
import java.text.MessageFormat

fun String.formatMessage(vararg args: Any): String = MessageFormat(this).format(args)

fun <T> List<T>.listPresentationInColumns(columns: Int, minColumnWidth: Int): String {
  val list = this
  return buildString {
    var pos = 0
    while (pos < list.size) {
      val subList = list.subList(pos, minOf(pos + columns, list.size))
      val row = subList.map { it.toString() }.joinToString(separator = "") { it.padEnd(minColumnWidth) }
      appendln(row)
      pos += columns
    }
  }
}

fun String.pluralizeWithNumber(times: Int): String = "$times " + this.pluralize(times)

fun String.pluralizeWithNumber(times: Long): String = pluralizeWithNumber(times.coerceToIntegerMax())

private fun Long.coerceToIntegerMax() = coerceAtMost(Integer.MAX_VALUE.toLong()).toInt()

private val knownPluralForms = mapOf(
    "this" to "these",
    "that" to "those",
    "is" to "are",
    "was" to "were"
)

fun String.pluralize(times: Long): String = pluralize(times.coerceToIntegerMax())

fun String.pluralize(times: Int): String {
  if (times < 0) throw IllegalArgumentException("Negative value")
  if (times == 1) {
    return this
  }
  return knownPluralForms[this] ?: English.plural(this, times)
}
