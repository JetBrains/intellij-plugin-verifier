package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.checkIde.CheckIdeResult

/**
 * @author Sergey Patrikeev
 */
data class CheckTrunkApiResult(val trunkResult: CheckIdeResult, val releaseResult: CheckIdeResult) : TaskResult
