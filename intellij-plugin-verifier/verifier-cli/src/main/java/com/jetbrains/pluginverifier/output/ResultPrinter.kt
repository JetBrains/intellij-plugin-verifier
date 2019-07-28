package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.PluginVerificationResult

interface ResultPrinter {
  fun printResults(results: List<PluginVerificationResult>)
}