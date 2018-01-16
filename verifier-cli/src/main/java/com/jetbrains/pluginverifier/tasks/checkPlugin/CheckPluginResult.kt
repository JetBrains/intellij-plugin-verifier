package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckPluginResult(invalidPluginFiles: List<InvalidPluginFile>,
                        val results: List<VerificationResult>) : TaskResult(invalidPluginFiles)
