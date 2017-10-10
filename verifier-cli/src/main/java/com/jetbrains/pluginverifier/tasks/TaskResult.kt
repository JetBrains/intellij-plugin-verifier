package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.repository.PluginRepository

interface TaskResult {
  fun printResults(outputOptions: OutputOptions, pluginRepository: PluginRepository)
}