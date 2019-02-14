package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckIdeResult(
    val ideVersion: IdeVersion,
    val results: List<VerificationResult>,
    val missingCompatibleVersionsProblems: List<MissingCompatibleVersionProblem>
) : TaskResult()
