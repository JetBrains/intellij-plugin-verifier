package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.tasks.TaskResult

data class CheckPluginResult(val results: List<Result>) : TaskResult
