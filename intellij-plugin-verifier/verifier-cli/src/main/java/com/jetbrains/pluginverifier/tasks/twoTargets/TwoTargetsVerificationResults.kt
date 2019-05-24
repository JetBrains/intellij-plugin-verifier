package com.jetbrains.pluginverifier.tasks.twoTargets

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.TaskResult

data class TwoTargetsVerificationResults(
    val baseTarget: VerificationTarget,
    val baseResults: List<VerificationResult>,
    val newTarget: VerificationTarget,
    val newResults: List<VerificationResult>
) : TaskResult()