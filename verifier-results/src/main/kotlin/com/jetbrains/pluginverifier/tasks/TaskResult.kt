package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.output.PrinterOptions

interface TaskResult {
  fun printResults(printerOptions: PrinterOptions)
}