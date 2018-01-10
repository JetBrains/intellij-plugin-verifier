package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckPluginResult(invalidPluginFiles: List<InvalidPluginFile>,
                        val results: List<Result>) : TaskResult(invalidPluginFiles)
