/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import org.atteo.evo.inflector.English
import java.text.MessageFormat
import java.time.Duration

private const val IN_SECOND = 1000
private const val IN_MINUTE = 60 * IN_SECOND
private const val IN_HOUR = 60 * IN_MINUTE
private const val IN_DAY = 24 * IN_HOUR

fun Duration.formatDuration(): String {
  var millis = toMillis()
  val days = millis / IN_DAY
  millis %= IN_DAY

  val hours = millis / IN_HOUR
  millis %= IN_HOUR

  val minutes = millis / IN_MINUTE
  millis %= IN_MINUTE

  val seconds = millis / IN_SECOND
  millis %= IN_SECOND

  if (days > 0) {
    return "$days d $hours h $minutes m"
  }
  if (hours > 0) {
    return "$hours h $minutes m $seconds s"
  }
  if (minutes > 0) {
    return "$minutes m $seconds s $millis ms"
  }
  if (seconds > 0) {
    return "$seconds s $millis ms"
  }
  return "$millis ms"
}

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
