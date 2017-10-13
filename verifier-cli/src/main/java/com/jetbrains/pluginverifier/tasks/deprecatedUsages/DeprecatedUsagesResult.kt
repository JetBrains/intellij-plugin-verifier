package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.tasks.TaskResult

data class DeprecatedUsagesResult(val ideVersion: IdeVersion, val results: List<Result>) : TaskResult
