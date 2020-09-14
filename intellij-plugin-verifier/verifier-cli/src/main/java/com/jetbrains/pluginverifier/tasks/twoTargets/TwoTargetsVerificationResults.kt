/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.twoTargets

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskResult

data class TwoTargetsVerificationResults(
  val baseTarget: PluginVerificationTarget,
  val baseResults: List<PluginVerificationResult>,
  val newTarget: PluginVerificationTarget,
  val newResults: List<PluginVerificationResult>
) : TaskResult {
  override fun createTaskResultsPrinter(pluginRepository: PluginRepository) = TwoTargetsResultPrinter()
}