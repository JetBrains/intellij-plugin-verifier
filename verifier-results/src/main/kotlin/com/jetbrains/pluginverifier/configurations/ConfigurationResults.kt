package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.output.PrinterOptions

interface ConfigurationResults {
  fun printResults(printerOptions: PrinterOptions)
}