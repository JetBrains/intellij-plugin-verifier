package com.jetbrains.pluginverifier.tasks.twoTargets

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.tasks.TaskResult

data class TwoTargetsVerificationResults(
  val baseTarget: PluginVerificationTarget,
  val baseResults: List<PluginVerificationResult>,
  val newTarget: PluginVerificationTarget,
  val newResults: List<PluginVerificationResult>
) : TaskResult()