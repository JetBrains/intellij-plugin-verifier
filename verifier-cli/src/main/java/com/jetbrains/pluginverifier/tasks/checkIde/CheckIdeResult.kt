package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckIdeResult(invalidPluginFiles: List<InvalidPluginFile>,
                     val ideVersion: IdeVersion,
                     val results: List<Result>,
                     val excludedPlugins: List<PluginIdAndVersion>,
                     val noCompatibleUpdatesProblems: List<MissingCompatibleUpdate>) : TaskResult(invalidPluginFiles)
