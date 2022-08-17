/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.plugin

sealed class PluginCreationResult<out PluginType : Plugin>

data class PluginCreationFail<out PluginType : Plugin>(val errorsAndWarnings: List<PluginProblem>) : PluginCreationResult<PluginType>() {
  constructor(error: PluginProblem) : this(listOf(error))

  override fun toString(): String = "Failed: ${errorsAndWarnings.joinToString()}"
}

data class PluginCreationSuccess<out PluginType : Plugin>(
  val plugin: PluginType,
  val warnings: List<PluginProblem>,
  val unacceptableWarnings: List<PluginProblem> = emptyList()
) :
  PluginCreationResult<PluginType>() {
  override fun toString(): String = "Success" +
    (if (unacceptableWarnings.isNotEmpty()) " but unacceptable warnings: " + unacceptableWarnings.joinToString() else "") +
    (if (warnings.isNotEmpty()) " but warnings: " + warnings.joinToString() else "")
}