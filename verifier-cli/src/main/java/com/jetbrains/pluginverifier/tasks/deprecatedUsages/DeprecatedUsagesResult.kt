package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.tasks.TaskResult

data class DeprecatedUsagesResult(val ideVersion: IdeVersion,
                                  val results: Map<PluginInfo, Set<DeprecatedApiUsage>>) : TaskResult
