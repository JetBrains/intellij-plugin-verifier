package com.jetbrains.pluginverifier.output

import com.jetbrains.pluginverifier.results.VerificationResult

interface ResultPrinter {
  fun printResults(results: List<VerificationResult>)
}