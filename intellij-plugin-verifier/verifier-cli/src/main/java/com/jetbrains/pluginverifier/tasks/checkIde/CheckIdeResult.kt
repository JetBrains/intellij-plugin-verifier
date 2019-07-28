package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckIdeResult(
    val ide: Ide,
    val results: List<PluginVerificationResult>,
    val missingCompatibleVersionsProblems: List<MissingCompatibleVersionProblem>
) : TaskResult()
