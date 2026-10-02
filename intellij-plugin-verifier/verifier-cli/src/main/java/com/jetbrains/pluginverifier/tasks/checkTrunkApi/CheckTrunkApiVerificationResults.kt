/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsVerificationResults

/**
 * An experiment with a possible replacement for [TwoTargetsVerificationResults] used by
 * [CheckTrunkApiTask].
 *
 * Keeps the release/trunk semantics explicit so check-trunk-api-specific reports can operate on the
 * trunk snapshot without guessing the target from an IDE version string.
 */
data class CheckTrunkApiVerificationResults(
  val releaseTarget: PluginVerificationTarget.IDE,
  val releaseResults: List<PluginVerificationResult>,
  val trunkTarget: PluginVerificationTarget.IDE,
  val trunkResults: List<PluginVerificationResult>
) : TaskResult {

  override fun createTaskResultsPrinter(pluginRepository: PluginRepository) = CheckTrunkApiResultPrinter()

  fun asTwoTargetsVerificationResults() = TwoTargetsVerificationResults(
    releaseTarget,
    releaseResults,
    trunkTarget,
    trunkResults
  )
}
