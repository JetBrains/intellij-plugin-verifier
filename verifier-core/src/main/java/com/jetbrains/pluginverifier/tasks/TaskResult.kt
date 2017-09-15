package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.output.PrinterOptions
import com.jetbrains.pluginverifier.repository.PluginRepository

interface TaskResult {
  fun printResults(printerOptions: PrinterOptions, pluginRepository: PluginRepository)
}