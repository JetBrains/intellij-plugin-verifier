package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckIdeResult(invalidPluginFiles: List<InvalidPluginFile>,
                     val ideVersion: IdeVersion,
                     val results: List<VerificationResult>,
                     val excludedPlugins: List<PluginIdAndVersion>,
                     val noCompatibleUpdatesProblems: List<MissingCompatibleUpdate>) : TaskResult(invalidPluginFiles)
