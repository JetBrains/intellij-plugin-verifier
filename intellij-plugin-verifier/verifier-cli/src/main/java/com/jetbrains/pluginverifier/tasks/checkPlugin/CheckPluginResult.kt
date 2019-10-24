package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckPluginResult(
  val invalidPluginFiles: List<InvalidPluginFile>,
  val results: List<PluginVerificationResult>
) : TaskResult()
