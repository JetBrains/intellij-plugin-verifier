package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckIdeResult(
    val ide: PluginVerificationTarget.IDE,
    val results: List<PluginVerificationResult>,
    val missingCompatibleVersionsProblems: List<MissingCompatibleVersionProblem>
) : TaskResult()
