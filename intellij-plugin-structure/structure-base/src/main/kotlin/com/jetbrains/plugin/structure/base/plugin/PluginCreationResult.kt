/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import java.io.Closeable

sealed class PluginCreationResult<out PluginType : Plugin>

data class PluginCreationFail<out PluginType : Plugin>(val errorsAndWarnings: List<PluginProblem>) : PluginCreationResult<PluginType>() {
  constructor(error: PluginProblem) : this(listOf(error))

  override fun toString(): String = "Failed: ${errorsAndWarnings.joinToString()}"
}

/**
 * Indicates a successfully constructed plugin without any [plugin errors][PluginProblem.Level.ERROR].
 *
 * @param plugin a successfully constructed plugin
 * @param warnings list of plugin problems with the [warning][PluginProblem.Level.WARNING]
 * @param unacceptableWarnings list of plugin problems with the [warning][PluginProblem.Level.UNACCEPTABLE_WARNING]
 * @param telemetry telemetry data when constructing this plugin
 * @param resources closeable resources, such as directory containing extracted contents of a ZIP plugin
 */
data class PluginCreationSuccess<out PluginType : Plugin>(
  val plugin: PluginType,
  val warnings: List<PluginProblem>,
  val unacceptableWarnings: List<PluginProblem> = emptyList(),
  val telemetry: PluginTelemetry = PluginTelemetry(),
  val resources: List<Closeable> = emptyList()
) :
  PluginCreationResult<PluginType>() {
  constructor(plugin: PluginType, problems: List<PluginProblem>) : this(
    plugin,
    problems.filter { it.level == PluginProblem.Level.WARNING },
    problems.filter { it.level == PluginProblem.Level.UNACCEPTABLE_WARNING }
  )

  override fun toString(): String = "Success" +
    (if (unacceptableWarnings.isNotEmpty()) " but unacceptable warnings: " + unacceptableWarnings.joinToString() else "") +
    (if (warnings.isNotEmpty()) " but warnings: " + warnings.joinToString() else "")
}