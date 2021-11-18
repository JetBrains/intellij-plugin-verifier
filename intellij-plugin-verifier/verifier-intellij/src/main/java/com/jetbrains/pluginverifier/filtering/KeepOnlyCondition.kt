/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import java.util.*

/**
 * Condition to keep only matching compatibility problems:
 * - pattern - RegExp pattern of the short description
 */
data class KeepOnlyCondition(
  val pattern: Regex
) {

  companion object {
    /**
     * Parses [KeepOnlyCondition] from this [line], that should contain only <keep_only_pattern>
     * Problems not matching the pattern will be ignored
     */
    fun parseCondition(line: String): KeepOnlyCondition {
      val token = line.trim()
      val parseRegexp = { s: String -> Regex(s, RegexOption.IGNORE_CASE) }
      return KeepOnlyCondition(parseRegexp(token))
    }
  }

  /**
   * Serializes this [KeepOnlyCondition] to [String].
   * It can be later deserialized via [parseCondition].
   */
  fun serializeCondition() = pattern.pattern

  override fun equals(other: Any?) = other is KeepOnlyCondition
    && pattern.pattern == other.pattern.pattern

  override fun hashCode() = Objects.hash(pattern.pattern)
}