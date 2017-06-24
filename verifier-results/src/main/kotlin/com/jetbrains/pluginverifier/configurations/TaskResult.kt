package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.output.PrinterOptions

interface TaskResult {
  fun printResults(printerOptions: PrinterOptions)
}