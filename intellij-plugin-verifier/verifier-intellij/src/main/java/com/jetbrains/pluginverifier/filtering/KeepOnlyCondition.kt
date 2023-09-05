/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import java.util.*

/**
 * Condition to keep only matching compatibility problems:
 * - pattern - RegExp pattern of the full description
 */
data class KeepOnlyCondition(
  val pluginId: Regex?,
  val version: Regex?,
  val pattern: Regex
) {

  companion object {
    private val USAGE = """
      Keep only problem line must be in the form: [<plugin_xml_id_regexp_pattern>[:<plugin_version_regexp_pattern>]:]<problem_description_regexp_pattern>
    Examples:
    org.some.*:3.*:access to unresolved class org.foo.Foo.*                           --- keep problems for plugins 'org.some.plugin', 'org.some.other.plugin' of versions 3+
    org.jetbrains.kotlin::access to unresolved class org.jetbrains.kotlin.compiler.*  --- keep problems for all versions of Kotlin plugin
    access to unresolved class org.jetbrains.kotlin.compiler.*                        --- keep problems for all plugins, but ignore others
    """.trimIndent()

    /**
     * Parses [KeepOnlyCondition] from this [line], which must be in one of the following forms:
     * 1) "<plugin-id-pattern>:<version-pattern>:<keep-only-pattern>"
     * 2) "<plugin-id-pattern>:<keep-only-pattern>"
     * 3) "<keep-only-pattern>"
     * Problems not matching the pattern will be ignored
     */
    fun parseCondition(line: String): KeepOnlyCondition {
      val tokens = line.split(":").map { it.trim() }
      val parseRegexp = { s: String -> Regex(s, RegexOption.IGNORE_CASE) }
      val parseRegexpOrNull = { s: String? -> s?.let { parseRegexp(it) } }
      return when (tokens.size) {
          1 -> KeepOnlyCondition(null, null, parseRegexp(tokens[0]))
          2 -> KeepOnlyCondition(parseRegexp(tokens[0]), null, parseRegexp(tokens[1]))
          3 -> KeepOnlyCondition(
            parseRegexpOrNull(tokens[0].takeIf { it.isNotEmpty() }),
            parseRegexpOrNull(tokens[1].takeIf { it.isNotEmpty() }),
            parseRegexp(tokens[2]))
          else -> throw IllegalArgumentException("Incorrect keep only problem line\n$line\n${USAGE}")
      }
    }
  }

  /**
   * Serializes this [KeepOnlyCondition] to [String].
   * It can be later deserialized via [parseCondition].
   */
  fun serializeCondition() = buildString {
    if (pluginId != null) {
      append(pluginId.pattern).append(":")
    }
    if (version != null) {
      append(version.pattern).append(":")
    }
    append(pattern.pattern)
  }

  override fun equals(other: Any?) = other is KeepOnlyCondition
          && pluginId?.pattern == other.pluginId?.pattern
          && version?.pattern == other.version?.pattern
          && pattern.pattern == other.pattern.pattern

  override fun hashCode() = Objects.hash(pluginId, version, pattern.pattern)
}